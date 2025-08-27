package it.pmcsn.lbsim.models.domain;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Job {
    private static final Logger logger = Logger.getLogger(Job.class.getName());
    private static long jobCounter = 0; // TODO: same as jobId???
    private final long jobId; // Unique identifier for the job
    private Server assignedServer; // The server to which this job is assigned
    private double remainingSize; // Remaining service demand in seconds to execute

    public Job(double size) {
        this.jobId = jobCounter++;
        this.assignedServer = null;
        this.remainingSize = size;
    }

    // Getters
    public long getJobId() {
        return jobId;
    }

    public double getRemainingSize() {
        return remainingSize;
    }

    public Server getAssignedServer() {
        return assignedServer;
    }

    // Setters
    public void assignServer(Server selectedServer) {
        if (selectedServer == null) {
            logger.log(Level.SEVERE, "Attempted to assign a null server to job {0}", jobId);
            throw new IllegalArgumentException("Assigned server cannot be null");
        }
        this.assignedServer = selectedServer;
    }

    public void setOriginalSize(double v) {
        if (v < 0) {
            logger.log(Level.SEVERE, "Attempted to assign negative size {0} to job {1}", new Object[]{v, jobId});
            throw new IllegalArgumentException("Job size cannot be negative");
        }
        this.remainingSize = v;
    }

    // Process the job, reducing its remaining size by the given amount
    public void process(double amount) {
        if (amount < 0) {
            logger.log(Level.SEVERE, "Attempted to process job {0} with negative amount {1}", new Object[]{jobId, amount});
            throw new IllegalArgumentException("Processing amount cannot be negative");
        }
        if (amount > remainingSize) {
            logger.log(Level.WARNING, "Processing amount {0} exceeds remaining size {1} for job {2}. Setting remaining size to zero.", new Object[]{amount, remainingSize, jobId});
            remainingSize = 0;
        } else {
            remainingSize -= amount;
        }
    }
}