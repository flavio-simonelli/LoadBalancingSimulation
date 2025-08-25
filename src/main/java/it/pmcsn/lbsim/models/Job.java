package it.pmcsn.lbsim.models;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Job {

    // Constants
    private static final Logger logger = Logger.getLogger(Job.class.getName());
    private static final double EPSILON = 1e-9;

    // Instance variables
    private final int jobId;
    private Server assignedServer;

    // Debug
    public boolean hadNegativeRemainingSize = false;

    /**
     * This is UNCORRELATED from the assigned server, but coincides numerically with the job service demand
     * to a WebServer with exclusive CPU access
     */
    private double remainingSize;

    // Constructor
    public Job(int jobId, double size) {
        this.jobId = jobId;
        this.assignedServer = null;
        this.remainingSize = size;
    }

    // Methods
    public void processForElapsedTime(double elapsedTime) {
        // Some basic validation
        if (assignedServer == null) {
            logger.log(Level.SEVERE,
                    "Attempted to process a job without an assigned server. jobId={0}",
                    jobId);
            throw new IllegalStateException("Job must be assigned to a server before processing");
        }

        if (assignedServer.getCurrentSi() <= 0) {
            logger.log(Level.SEVERE,
                    "Attempted to process a job on a server with zero load. jobId={0}, serverSi={1}",
                    new Object[]{jobId, assignedServer.getCurrentSi()});
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double effectiveProcessingRate = assignedServer.getCpuPercentage() *
                assignedServer.getCpuMultiplier() /
                assignedServer.getCurrentSi();

        double serviceReceived = elapsedTime * effectiveProcessingRate;

        // Update remaining size
        remainingSize -= serviceReceived;
        logger.log(Level.INFO,
                "Job {0} processed for {1} seconds, remaining service demand: {2}\n",
                new Object[]{jobId, Double.toString(elapsedTime), Double.toString(remainingSize)});
        if (remainingSize < 0) {
            logger.log(Level.WARNING,
                    "Job {0} has negative remaining size: {1}. Setting to zero.\n",
                    new Object[]{jobId, Double.toString(remainingSize)});
            hadNegativeRemainingSize = true;
            remainingSize = 0;
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

    public void assignServer(Server selectedServer) {
        if (selectedServer == null) {
            logger.log(Level.SEVERE,
                    "Attempted to assign a null server to job {0}",
                    jobId);
            throw new IllegalArgumentException("Assigned server cannot be null");
        }
        this.assignedServer = selectedServer;
    }

    public void setRemainingSize(double v) {
        if (v < 0) {
            logger.log(Level.SEVERE,
                    "Tentativo di impostare remainingSize negativo: {0} per job {1}",
                    new Object[]{v, jobId});
            throw new IllegalArgumentException("remainingSize non può essere negativo");
        }
        this.remainingSize = v;
    }
}