package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;


import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final Rngs rngs;
    private final Rvgs rvgs;                        // Random Variate Generator

    // Load balancer
    private final LoadBalancer loadBalancer;

    // Interarrival variate parameters
    private final HyperExponential interarrivalTimeObj;

    // Service variate parameters
    private final HyperExponential serviceTimeObj;

    private final CsvAppender csvAppenderServers;
    private final CsvAppender csvAppenderDepartures;
    private final CsvAppender csvAppenderJobs;

    // Debug
    public int jobWithNegativeRemainingSize = 0;

    // Constructor
    public Simulator(boolean isFirstSimulation,
                     long seed0,
                     long seed1,
                     long seed2,
                     long seed3,
                     long seed4,
                     long seed5,
                     int SImax,
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
            this.csvAppenderJobs = new CsvAppender(Path.of(csvExportDir + "Jobs.csv"), "IdJob", "Arrival", "Departure", "ResponseTime","Response-(Departure-Arrival)", "OriginalSize", "processedBySpike");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            this.csvAppenderServers = new CsvAppender(Path.of(csvExportDir + "Servers.csv"), "timestamp", "active_jobs_per_webserver", "active_web_servers", "active_jobs_spikeserver");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            this.csvAppenderDepartures = new CsvAppender(Path.of(csvExportDir + "Departures.csv"), "timestamp", "jobs_per_server", "active_servers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Initialize simulation state
        this.currentTime = 0.0;
        this.jobStats = new ArrayList<>();

        // Initialize random number generators
        this.rngs = new Rngs();
        this.rvgs = new Rvgs(rngs);

        if (isFirstSimulation) {
            rngs.plantSeeds(seed0);
        } else {
            rngs.plantSeeds(-1); // only for initialize
            rngs.selectStream(0);
            rngs.putSeed(seed0);
            rngs.selectStream(1);
            rngs.putSeed(seed1);
            rngs.selectStream(2);
            rngs.putSeed(seed2);
            rngs.selectStream(3);
            rngs.putSeed(seed3);
            rngs.selectStream(4);
            rngs.putSeed(seed4);
            rngs.selectStream(5);
            rngs.putSeed(seed5);
        }
        logger.log(Level.SEVERE, "Initial seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

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
        while (!jobStats.isEmpty()){
            guessNextEvent();
        }

        logger.log(Level.INFO, "Simulation completed at time {0}\n", currentTime);
        logger.log(Level.INFO, "Total jobs processed: {0}\n", jobIdCounter);
        logger.log(Level.INFO, "Total jobs with negative remaining size: {0}\n", jobWithNegativeRemainingSize);
        logger.log(Level.INFO, "Negative percentage is {0,number,0.000}%\n", (100.0 * jobWithNegativeRemainingSize) / jobIdCounter);
        logger.log(Level.SEVERE, "Final seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

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
            if (js.getJob().hadNegativeRemainingSize) {
                jobWithNegativeRemainingSize++;
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

        // csv logging
        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsPerWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );


        // Generate next arrival time
        genNextInterarrivalTime();
    }

    private void departureHandler(JobStats targetDepartureJobStats) {
        // Process elapsed time for all active jobs
        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime(targetDepartureJobStats.getEstimatedDepartureTime() - currentTime);
            //debugging
            if (js.getJob().hadNegativeRemainingSize) {
                jobWithNegativeRemainingSize++;
            }
        }

        // Update current time to departure time
        this.currentTime = targetDepartureJobStats.getEstimatedDepartureTime();

        // Process job departure through load balancer
        double responseTime = this.currentTime - targetDepartureJobStats.getArrivalTime();

        this.loadBalancer.departureJob(targetDepartureJobStats.getJob(), responseTime, this.currentTime);

        // Add to the csv for forensics analysis
        this.csvAppenderJobs.writeRow(
                    String.valueOf(targetDepartureJobStats.getJob().getJobId()),                                 // job id
                    String.valueOf(targetDepartureJobStats.getArrivalTime()),                                    // arrival time
                    String.valueOf(currentTime),                                                                 // departure time
                    String.valueOf(responseTime),                                                                // response time
                    String.valueOf(responseTime - (currentTime - targetDepartureJobStats.getArrivalTime())),                   // response - (departure - arrival)
                    String.valueOf(targetDepartureJobStats.getOriginalSize()) ,                                  // original size
                    String.valueOf(targetDepartureJobStats.getJob().getAssignedServer().getCpuMultiplier()>1) // processed by spike
                    );

        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsPerWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
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