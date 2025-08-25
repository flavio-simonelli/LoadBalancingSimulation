package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;


import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {

    // Constants
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());
    private static final double EPSILON = 1e-9;

    // Instance variables - Simulation state
    private Double currentTime;                     // Current simulation time
    private int jobIdCounter = 0;                   // Counter for unique job IDs
    private final List<JobStats> jobStats;          // List of jobsStats
    private double nextArrivalTime;                 // Next arrival time for jobs

    private final Rvgs rvgs;                        // Random Variate Generator

    // Load balancer
    private final LoadBalancer loadBalancer;

    // Interarrival variate parameters
    private final HyperExponential interarrivalTimeObj;

    // Service variate parameters
    private final HyperExponential serviceTimeObj;

    private final CsvAppender csvAppenderJobs;

    //debugging
    public int simErrorCount = 0;
    public List<Job> errorJobs = new ArrayList<>();

    // Constructor
    public Simulator(int SImax,
                     int SImin,
                     double R0max,
                     double R0min,
                     int initialServerCount,
                     int cpuMultiplierSpike,
                     double cpuPercentageSpike,
                     int slidingWindowSize,
                     SchedulingType schedulingType,
                     double horizontalScalingCoolDown,
                     double interarrivalCv,
                     double interarrivalMean,
                     double serviceCv,
                     double serviceMean,
                     String csvExportDir) {

        // initialize path csv
        try {
            this.csvAppenderJobs = new CsvAppender(Path.of(csvExportDir + "Jobs.csv"), "IdJob", "Arrival", "Departure", "ResponseTime", "size", "processedBySpike");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Initialize simulation state
        this.currentTime = 0.0;
        this.jobStats = new ArrayList<>();

        // Initialize random number generators
        Rngs rngs = new Rngs();
        rngs.plantSeeds(rngs.DEFAULT); // TODO: make it configurable
        this.rvgs = new Rvgs(rngs);

        // Initialize hyperexponential distribution
        this.interarrivalTimeObj = new HyperExponential(interarrivalCv, interarrivalMean);
        logger.log(Level.INFO, "Hyperexponential interarrival with parameters {0} {1} {2}\n", new Object[]{this.interarrivalTimeObj.getP(), this.interarrivalTimeObj.getM1(), this.interarrivalTimeObj.getM2()});
        this.serviceTimeObj = new HyperExponential(serviceCv, serviceMean);
        logger.log(Level.INFO, "Hyperexponential service with parameters {0} {1} {2}\n", new Object[]{this.serviceTimeObj.getP(), this.serviceTimeObj.getM1(), this.serviceTimeObj.getM2()});

        // Initialize load balancer
        this.loadBalancer = new LoadBalancer(initialServerCount,
                cpuMultiplierSpike,
                cpuPercentageSpike,
                slidingWindowSize,
                SImax,
                SImin,
                R0max,
                R0min,
                schedulingType,
                horizontalScalingCoolDown);
    }

    // Methods
    public void run(double simulationDuration) {
        if (simulationDuration <= 0.0) {
            logger.log(Level.SEVERE, "Simulation Duration must be greater than zero");
            throw new IllegalArgumentException("Simulation duration must be greater than zero");
        }

        genNextInterarrivalTime();

        // Main simulation loop - process events until simulation duration
        do {
            guessNextEvent();
        } while (currentTime < simulationDuration);

        // Drain remaining jobs after simulation ends
        this.nextArrivalTime = Double.POSITIVE_INFINITY;
        do {
            guessNextEvent();
        } while (!jobStats.isEmpty());

        logger.log(Level.INFO, "Simulation completed at time {0}", currentTime);
        //debugging
        logger.log(Level.INFO, "Simulation encountered {0} numerical errors", simErrorCount);
        if(!errorJobs.isEmpty()){
            logger.log(Level.WARNING, "Jobs with numerical errors:");
            for (Job job : errorJobs) {
                logger.log(Level.WARNING, "Job ID: {0}, Error Count: {1}", new Object[]{job.getJobId(), job.jobErrorCount});
            }
        }
        csvAppenderJobs.close();
    }

    private void guessNextEvent() {
        double nextDepartureTime = Double.POSITIVE_INFINITY;
        JobStats jobStat = null;

        // Find the next job to depart
        for (JobStats stats : jobStats) {
            double depTime = stats.getEstimatedDepartureTime();
            if (depTime < nextDepartureTime) {
                nextDepartureTime = depTime;
                jobStat = stats;
            }
        }

        // Process the next event (arrival or departure)
        if (nextArrivalTime >= nextDepartureTime) {
            departureHandler(jobStat);
        } else {
            arrivalHandler();
        }
    }

    private void arrivalHandler() {
        // Process elapsed time for all active jobs
        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime(nextArrivalTime - currentTime);
            //debugging
            if(js.getJob().jobErrorCount > 0){
                int idx = errorJobs.indexOf(js.getJob());
                if (idx == -1) {
                    errorJobs.add(js.getJob());
                } else {
                    Job existingJob = errorJobs.get(idx);
                    if (existingJob.jobErrorCount < js.getJob().jobErrorCount) {
                        errorJobs.set(idx, js.getJob());
                    }
                }
            }
        }

        // Update current time to arrival time
        this.currentTime = nextArrivalTime;

        // Create new job and assign it to load balancer
        if (rvgs == null) {
            logger.log(Level.SEVERE, "Random Variate Generator (Rvgs) is not initialized");
            throw new IllegalStateException("Rvgs must be initialized");
        }
        int sStreamP = 3;
        int sStreamExp1 = 4;
        int sStreamExp2 = 5;
        double size = rvgs.hyperExponential(this.serviceTimeObj.getP(), this.serviceTimeObj.getM1(), this.serviceTimeObj.getM2(), sStreamP, sStreamExp1, sStreamExp2);
        JobStats newJobStats = new JobStats(new Job(jobIdCounter++, size), this.currentTime, size);
        this.loadBalancer.jobAssignment(newJobStats.getJob());
        this.jobStats.add(newJobStats);

        // Recalculate estimated departure times for all jobs
        for (JobStats jobStat : jobStats) {
            jobStat.estimateDepartureTime(this.currentTime);
        }

        // Generate next arrival time
        genNextInterarrivalTime();
    }

    private void departureHandler(JobStats targetDepartureJobStats) {
        // Process elapsed time for all active jobs
        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime(targetDepartureJobStats.getEstimatedDepartureTime() - currentTime);
            //debugging
            if(js.getJob().jobErrorCount > 0){
                int idx = errorJobs.indexOf(js.getJob());
                if (idx == -1) {
                    errorJobs.add(js.getJob());
                } else {
                    Job existingJob = errorJobs.get(idx);
                    if (existingJob.jobErrorCount < js.getJob().jobErrorCount) {
                        errorJobs.set(idx, js.getJob());
                    }
                }
            }
        }

        // Update current time to departure time
        this.currentTime = targetDepartureJobStats.getEstimatedDepartureTime();

        // Validate that job is fully processed
        double remSize = targetDepartureJobStats.getJob().getRemainingSize();
        if (Math.abs(remSize) > EPSILON) {
            if (Math.abs(remSize) < 10 * EPSILON) {
                logger.log(Level.WARNING,
                        "Piccolo errore numerico: remaining size di job {0} è {1,number,0.000} (correggo a zero)",
                        new Object[]{
                                targetDepartureJobStats.getJob().getJobId(),
                                remSize
                        });
                // Correggi a zero
                targetDepartureJobStats.getJob().setRemainingSize(0.0);
                simErrorCount++;
            } else {
                logger.log(Level.WARNING,
                        "Remaining size of job {0} is not zero but {1,number,0.000}",
                        new Object[]{
                                targetDepartureJobStats.getJob().getJobId(),
                                remSize
                        });
                throw new IllegalStateException("Job is departed, but his remaining size is not zero!!!");
            }
        }

        // Process job departure through load balancer
        double responseTime = this.currentTime - targetDepartureJobStats.getArrivalTime();

        this.loadBalancer.departureJob(targetDepartureJobStats.getJob(), responseTime, this.currentTime);

        // Add to the csv for forensics analysis
        this.csvAppenderJobs.writeRow(
                    String.valueOf(targetDepartureJobStats.getJob().getJobId()),                                 // job id
                    String.valueOf(targetDepartureJobStats.getArrivalTime()),                                    // arrival time
                    String.valueOf(currentTime),                                                                 // departure time
                    String.valueOf(responseTime),                                                                // response time
                    String.valueOf(targetDepartureJobStats.getOriginalSize()) ,                                  // original size
                    String.valueOf(targetDepartureJobStats.getJob().getAssignedServer().getCpuMultiplier()>1) // processed by spike
                    );

        this.jobStats.remove(targetDepartureJobStats);

        // Recalculate estimated departure times for remaining jobs
        for (JobStats js : this.jobStats) {
            js.estimateDepartureTime(this.currentTime);
        }
    }

    private void genNextInterarrivalTime() {
        if (rvgs == null) {
            logger.log(Level.SEVERE, "Random Variate Generator (Rvgs) is not initialized");
            throw new IllegalStateException("Rvgs must be initialized");
        }

        int iStreamP = 0;
        int iStreamExp1 = 1;
        int iStreamExp2 = 2;
        double interarrivalTime = rvgs.hyperExponential(this.interarrivalTimeObj.getP(), this.interarrivalTimeObj.getM1(), this.interarrivalTimeObj.getM2() , iStreamP, iStreamExp1, iStreamExp2);
        this.nextArrivalTime = this.currentTime + interarrivalTime;
    }
}