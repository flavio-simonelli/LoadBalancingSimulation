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
    private Server assignedServer;
    private final double totalServiceDemand;      // Original processing time required
    private double remainingServiceDemand;        // How much processing is left
    private boolean isCompleted;                  // Whether the job has finished processing

    /**
     * Constructor for creating a new job
     * @param jobId Unique identifier for this job
     * @param totalServiceDemand Total processing time required (assuming exclusive CPU access)
     */
    public Job(int jobId, double totalServiceDemand) {
        this.jobId = jobId;
        this.totalServiceDemand = totalServiceDemand;
        this.remainingServiceDemand = totalServiceDemand;
        this.isCompleted = false;
        this.assignedServer = null;
    }

    /**
     * Processes this job for a given elapsed time period, considering processor sharing.
     * The actual service received depends on how many jobs are currently sharing the CPU.
     * It is mandatory to assign this job to a server before calling this method.
     *
     * @param elapsedTime Real time that passed in the simulation
     */
    public void processForElapsedTime(double elapsedTime) {
        if (isCompleted) {
            logger.log(Level.WARNING, "Attempted to process a completed job. jobId={0}", jobId);
            return;
        }
        if (assignedServer == null) {
            logger.log(Level.WARNING, "Attempted to process a job without an assigned server. jobId={0}", jobId);
            return;
        }

        int currentServerLoad = assignedServer.getCurrentSi();
        if (currentServerLoad <= 0) {
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double effectiveProcessingRate = 1.0 / currentServerLoad;
        double actualServiceReceived = elapsedTime * effectiveProcessingRate;

        // Reduce remaining service demand by actual service received
        remainingServiceDemand = Math.max(0, remainingServiceDemand - actualServiceReceived);

        // Mark as completed if no demand remains
        if (remainingServiceDemand <= 0) {
            isCompleted = true;
        }
    }

    // Getters
    public int getJobId() {
        return jobId;
    }

    public Server getAssignedServer() {
        return assignedServer;
    }

    public double getTotalServiceDemand() {
        return totalServiceDemand;
    }

    public double getRemainingServiceDemand() {
        return remainingServiceDemand;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    // Setters
    public void setAssignedServer(Server server) {
        this.assignedServer = server;
    }
}