package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.libs.Rngs;
import it.pmcsn.lbsim.libs.Rvgs;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());
    private static final double EPSILON = 1e-9;

    private Double currentTime;                 // Current simulation time
    private Double simulationDuration;          // Total duration of the simulation
    private int jobIdCounter = 0;               // Counter for unique job IDs
    private List<JobStats> jobStats;            // List of jobsStats
    private Rngs rngs;                          // Random Number Generator
    private Rvgs rvgs;                          // Random Variate Generator
    private double nextArrivalTime;        // Next arrival time for jobs

    private LoadBalancer loadBalancer;        // Load Balancer instance

    // Interarrival variate parameters
    double iMean = 0.15;
    double iCv = 4;
    int iStreamP = 0;
    int iStreamExp1 = 1;
    int iStreamExp2 = 2;

    // Services variate parameters
    double sMean = 0.16;
    double sCv = 4;
    int sStreamP = 3;
    int sStreamExp1 = 4;
    int sStreamExp2 = 5;

    public Simulator(int SImax, int SImin, double R0max, double R0min, int initialServerCount, int cpuMultiplierSpike, double cpuPercentageSpike, int slidingWindowSize, SchedulingType schedulingType) {
        this.currentTime = 0.0;                  // Initialize current time to zero
        this.jobStats = new ArrayList<>();               // Initialize jobStats list
        this.rngs = new Rngs();                  // Initialize the Random Variate Generator
        rngs.plantSeeds(rngs.DEFAULT);
        this.rvgs = new Rvgs(rngs);              // Initialize the Random Variate Generator

        this.loadBalancer = new LoadBalancer(initialServerCount, cpuMultiplierSpike, cpuPercentageSpike, slidingWindowSize, SImax, SImin, R0max, R0min, schedulingType);
    }

    public void run(double simulationDuration) {
        if (simulationDuration <= 0.0) {
            logger.log(Level.SEVERE, "Simulation Duration must be greater than zero");
            throw new IllegalArgumentException("Simulation duration must be greater than zero");
        }
        this.simulationDuration = simulationDuration; // Set the total duration of the simulation

        genNextInterarrivalTime();

        do {
            guessNextEvent();
        } while (currentTime < simulationDuration);
        this.nextArrivalTime = Double.POSITIVE_INFINITY; // Set next arrival time to infinity after simulation ends
        do {
            guessNextEvent();
        } while (!jobStats.isEmpty());
        logger.log(Level.INFO, "Simulation completed at time {0}", currentTime);
    }

    private void guessNextEvent(){
        double nexDepartureTime = Double.POSITIVE_INFINITY;
        JobStats jobStat = null;
        //prima cosa calcolo il tempo di partenza del prossimo job
        for(JobStats stats : jobStats){
            double depTime = stats.getEstimatedDepartureTime();
            if (depTime < nexDepartureTime) {
                nexDepartureTime = depTime;
                jobStat = stats;
            }
        }
        if (nextArrivalTime >= nexDepartureTime) {
           // this.currentTime = nexDepartureTime;
            departureHandler(jobStat);
        }else{
           // this.currentTime = nextArrivalTime;
            arrivalHandler();
        }

        // controlla tra il prossimo arrivo e la prossima partenza quella che avviene prima
        // setti il current time al prossimo evento scelto
        // chiama arrivalHandler() o departureHandler() a seconda di quale evento abbiamo scelto
    }

    private void arrivalHandler() {
        // crea il nuovo job generando anche la sua size
        // genera le sue stats e le mette nella lista jobStats
        // passare il nuovo job al load balancer
        // aggiornare le remaining size dei job attualmente in esecuzione e le loro conseguenti estimated departure time
        // generare il prossimo tempo di arrivo genNextInterarrivalTime()

        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime(nextArrivalTime - currentTime);
        }

        this.currentTime = nextArrivalTime;

        JobStats JobStats = new JobStats(genJob(), this.currentTime);
        this.loadBalancer.jobAssignment(JobStats.getJob());
        this.jobStats.add(JobStats);

        for (JobStats jobStat : jobStats) {
            jobStat.estimateDepartureTime(this.currentTime);
        }

        genNextInterarrivalTime();
    }

    private void departureHandler(JobStats departureJobStats) {
        // scarica il tempo del job rimasto
        // aggiornare le remaining size dei job attualmente in esecuzione e le loro conseguenti estimated departure time
        // passare il response time al load balancer

        for (JobStats js : this.jobStats) {
            js.getJob().processForElapsedTime( departureJobStats.getEstimatedDepartureTime() - currentTime);
        }

        this.currentTime = departureJobStats.getEstimatedDepartureTime();

        if (Math.abs(departureJobStats.getJob().getRemainingSize()) > EPSILON) {
            logger.log(Level.WARNING,
                    "Remaining size of job {0} is not zero but {1,number,0.000}",
                    new Object[]{
                            departureJobStats.getJob().getJobId(),
                            departureJobStats.getJob().getRemainingSize()
                    });
            throw new IllegalStateException("Job is departed, but his remaining size is not zero!!!");
        }

        this.loadBalancer.departureJob(departureJobStats.getJob(),this.currentTime - departureJobStats.getArrivalTime());
        this.jobStats.remove(departureJobStats);

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

        this.nextArrivalTime = this.currentTime + rvgs.hyperExponentialFromMeanCV(iMean, iCv, iStreamP, iStreamExp1, iStreamExp2);
        //per ora conservo il concetto di istante di tempo assoluto che può essere utile per le statistiche
    }




}
