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

    /**
     * This is UNCORRELATED from the assigned server, but coincides numerically with the job service demand
     * to a WebServer with exclusive CPU access
     */
    private double remainingSize;


    public Job(int jobId, double size) {
        this.jobId = jobId;
        this.remainingSize = size;
        this.assignedServer = null;
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
        double effectiveProcessingRate = assignedServer.getCpuPercentage() * assignedServer.getCpuMultiplier() / currentServerSi;
        double serviceReceived = elapsedTime * effectiveProcessingRate ;

        // Reduce remaining service demand by actual service received
        if (serviceReceived > remainingSize) {
            logger.log(Level.SEVERE, "Service received {0} exceeds remaining service demand {1} for job {2}", new Object[]{serviceReceived, remainingSize, jobId});
            throw new IllegalStateException("Service received exceeds remaining service demand");
        } else {
            remainingSize -= serviceReceived;
            logger.log(Level.INFO, "Job {0} processed for {1} seconds, remaining service demand: {2}", new Object[]{jobId, elapsedTime, remainingSize});
        }
    }

    // Getters and Setters
    public int getJobId() {
        return jobId;
    }

    public double getRemainingSize() {
        return remainingSize;
    }

    public Server getAssignedServer() {
        return assignedServer;
    }

    public void setAssignedServer(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("Assigned server cannot be null");
        }
        this.assignedServer = server;
    }

    public void assignServer(Server selectedServer) {
        if (selectedServer == null) {
            logger.log(Level.SEVERE, "Attempted to assign a null server to job {0}", jobId);
            throw new IllegalArgumentException("Assigned server cannot be null");
        }
        this.assignedServer = selectedServer;
    }
}