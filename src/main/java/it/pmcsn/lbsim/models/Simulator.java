package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.libs.random.Rngs;
import it.pmcsn.lbsim.libs.random.Rvgs;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {

    // Constants
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());
    private static final double EPSILON = 1e-9;
    private static final long MYSEED = -1L;

    // Instance variables - Simulation state
    private Double currentTime;                     // Current simulation time
    private Double simulationDuration;              // Total duration of the simulation
    private int jobIdCounter = 0;                   // Counter for unique job IDs
    private List<JobStats> jobStats;                // List of jobsStats
    private double nextArrivalTime;                 // Next arrival time for jobs

    // Random number generators
    private Rngs rngs;                              // Random Number Generator
    private Rvgs rvgs;                              // Random Variate Generator

    // Load balancer
    private LoadBalancer loadBalancer;              // Load Balancer instance

    // Interarrival variate parameters
    private double iMean = 0.15;
    private double iCv = 4;
    private int iStreamP = 0;
    private int iStreamExp1 = 1;
    private int iStreamExp2 = 2;

    // Service variate parameters
    private double sMean = 0.30;
    private double sCv = 4;
    private int sStreamP = 3;
    private int sStreamExp1 = 4;
    private int sStreamExp2 = 5;

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
                     double horizontalScalingCoolDown) {

        // Initialize simulation state
        this.currentTime = 0.0;
        this.jobStats = new ArrayList<>();

        // Initialize random number generators
        this.rngs = new Rngs();
        rngs.plantSeeds(MYSEED); //TODO: not working anymore
        this.rvgs = new Rvgs(rngs);

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

        this.simulationDuration = simulationDuration;
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
        }

        // Update current time to arrival time
        this.currentTime = nextArrivalTime;

        // Create new job and assign it to load balancer
        JobStats newJobStats = new JobStats(genJob(), this.currentTime);
        this.loadBalancer.jobAssignment(newJobStats.getJob());
        this.jobStats.add(newJobStats);

        // Recalculate estimated departure times for all jobs
        for (JobStats jobStat : jobStats) {
            jobStat.estimateDepartureTime(this.currentTime);
        }

        // Generate next arrival time
        genNextInterarrivalTime();
    }

    private void departureHandler(JobStats departureJobStats) {
        // Process elapsed time for all active jobs
        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime(departureJobStats.getEstimatedDepartureTime() - currentTime);
        }

        // Update current time to departure time
        this.currentTime = departureJobStats.getEstimatedDepartureTime();

        // Validate that job is fully processed
        if (Math.abs(departureJobStats.getJob().getRemainingSize()) > EPSILON) {
            logger.log(Level.WARNING,
                    "Remaining size of job {0} is not zero but {1,number,0.000}",
                    new Object[]{
                            departureJobStats.getJob().getJobId(),
                            departureJobStats.getJob().getRemainingSize()
                    });
            throw new IllegalStateException("Job is departed, but his remaining size is not zero!!!");
        }

        // Process job departure through load balancer
        double responseTime = this.currentTime - departureJobStats.getArrivalTime();
        try {
            this.loadBalancer.departureJob(departureJobStats,departureJobStats.getJob(), responseTime, this.currentTime);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.jobStats.remove(departureJobStats);

        // Recalculate estimated departure times for remaining jobs
        for (JobStats js : this.jobStats) {
            js.estimateDepartureTime(this.currentTime);
        }
    }

    private Job genJob() {
        if (rvgs == null) {
            logger.log(Level.SEVERE, "Random Variate Generator (Rvgs) is not initialized");
            throw new IllegalStateException("Rvgs must be initialized");
        }

        double size = rvgs.hyperExponentialFromMeanCV(sMean, sCv, sStreamP, sStreamExp1, sStreamExp2);
        return new Job(jobIdCounter++, size);
    }

    private void genNextInterarrivalTime() {
        if (rvgs == null) {
            logger.log(Level.SEVERE, "Random Variate Generator (Rvgs) is not initialized");
            throw new IllegalStateException("Rvgs must be initialized");
        }

        double interarrivalTime = rvgs.hyperExponentialFromMeanCV(iMean, iCv, iStreamP, iStreamExp1, iStreamExp2);
        this.nextArrivalTime = this.currentTime + interarrivalTime;
    }
}