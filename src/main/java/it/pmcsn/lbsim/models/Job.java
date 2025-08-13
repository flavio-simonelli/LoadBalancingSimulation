package it.pmcsn.lbsim.models;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a job in an event-driven processor sharing scheduling simulation.
 * Job state changes only through discrete events managed by the simulation scheduler.
 */
public class Job {
    private static final Logger logger = Logger.getLogger(Job.class.getName());
    private final int jobId;

    /**
     * This is UNCORRELATED from the assigned server, but coincides numerically with the job service demand
     * to a WebServer with exclusive CPU access
     */
    private final double size;

    private double remainingSize;
    private Server assignedServer;

    /**
     * Arrival time of the job in the system, used for statistics
     */
    //TODO: should be the arrival at server instead?
    private double arrivalTime;

    /**
     * Estimated departure time of the job in the system, used for calculate the next departure event
     * in simulation (and for statistical results)
     */
    private double currentEstimatedDeparture;

    public Job(int jobId, double size, double arrivalTime) {
        this.jobId = jobId;
        this.size = size;
        this.remainingSize = size;
        this.arrivalTime = arrivalTime;
        this.assignedServer = null;
    }

    //TODO: param currentTime never used, remove?
    public void assignServer(Server server, double currentTime) {
        if (server == null) {
            throw new IllegalArgumentException("Assigned server cannot be null");
        }
        this.assignedServer = server;
    }

    public void processForElapsedTime(double elapsedTime) {
        if (assignedServer == null) {
            logger.log(Level.SEVERE, "Attempted to process a job without an assigned server. jobId={0}", jobId);
            throw new IllegalStateException("Job must be assigned to a server before processing");
        }

        int currentServerSi = assignedServer.getCurrentSi();
        if (currentServerSi <= 0) {
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double effectiveProcessingRate = assignedServer.getCpuPercentage() * assignedServer.getCpuMultiplier() /
                currentServerSi;
        double serviceReceived = elapsedTime * effectiveProcessingRate ;

        // Reduce remaining service demand by actual service received
        if (serviceReceived > remainingSize) {
            logger.log(Level.SEVERE, "Service received {0} exceeds remaining service demand {1} for job {2}",
                    new Object[]{serviceReceived, remainingSize, jobId});
            throw new IllegalStateException("Service received exceeds remaining service demand");
        } else {
            remainingSize -= serviceReceived;
            logger.log(Level.INFO, "Job {0} processed for {1} seconds, remaining service demand: {2}",
                    new Object[]{jobId, elapsedTime, remainingSize});
        }
    }

    // TODO: check if move to simulator model
    //TODO: should evaluate a time span or a timestamp? I think timestamp should be only inside the Simulator model
    public void calculateDepartureTime(double currentTime) {
        if (assignedServer == null) {
            logger.log(Level.SEVERE, "Attempted to recalculate remaining service demand without " +
                    "an assigned server. jobId={0}", jobId);
            throw new IllegalStateException("Job must be assigned to a server before recalculating" +
                    " remaining service demand");
        }

        int currentServerSi = assignedServer.getCurrentSi();
        if (currentServerSi <= 0) {
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double effectiveProcessingRate = assignedServer.getCpuPercentage() * assignedServer.getCpuMultiplier() /
                currentServerSi;
        this.currentEstimatedDeparture = currentTime + remainingSize / effectiveProcessingRate;
        logger.log(Level.INFO, "departure time for job {0} recalculated: {1}",
                new Object[]{jobId, this.currentEstimatedDeparture});
    }
}