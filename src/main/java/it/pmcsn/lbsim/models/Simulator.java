package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.libs.Rngs;
import it.pmcsn.lbsim.libs.Rvgs;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());

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

    public Simulator(int SImax, int SImin, double R0max, double R0min, int initialServerCount, int cpuMultiplierSpike, double cpuPercentageSpike, int slidingWindowSize) {
        this.currentTime = 0.0;                  // Initialize current time to zero
        this.jobStats = List.of();               // Initialize jobStats list
        this.rngs = new Rngs();                  // Initialize the Random Variate Generator
        this.rvgs = new Rvgs(rngs);              // Initialize the Random Variate Generator

        this.loadBalancer = new LoadBalancer(initialServerCount, cpuMultiplierSpike, cpuPercentageSpike, slidingWindowSize, SImax, SImin, R0max, R0min);
    }

    public void run(double simulationDuration) {
        if (simulationDuration <= 0.0) {
            logger.log(Level.SEVERE, "Simulation Duration must be greater than zero");
            throw new IllegalArgumentException("Simulation duration must be greater than zero");
        }
        genNextInterarrivalTime();
        this.currentTime = nextArrivalTime;
        JobStats firstJobStats = new JobStats(genJob(), this.currentTime);

        do {
            genNextInterarrivalTime();
            // if some of the current jobs ends before next event then...
        } while (currentTime < simulationDuration);
    }

    private void guessNextEvent(){
        // controlla tra il prossimo arrivo e la prossima partenza quella che avviene prima
        // setti il current time al prossimo evento scelto
        // chiama arrivalHandler() o departureHandler() a seconda di quale evento abbiamo scelto
    }

    private void arrivalHandeler() {
        // crea il nuovo job generando anche la sua size
        // genera le sue stats e le mette nella lista jobStats
        // passare il nuovo job al load balancer
        // aggiornare le remaining size dei job attualmente in esecuzione e le loro conseguenti estimated departure time
        // generare il prossimo tempo di arrivo
    }

    private void departureHandler() {
        // scarica il tempo del job rimasto
        // aggiornare le remaining size dei job attualmente in esecuzione e le loro conseguenti estimated departure time
        // passare il response time al load balancer
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

        this.nextArrivalTime = rvgs.hyperExponentialFromMeanCV(iMean, iCv, iStreamP, iStreamExp1, iStreamExp2);
    }




}
