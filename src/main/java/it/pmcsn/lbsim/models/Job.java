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

    /**
     * This is UNCORRELATED from the assigned server, but coincides numerically with the job service demand
     * to a WebServer with exclusive CPU access
     */
    private double remainingSize;

    //debugging
    public int jobErrorCount = 0;

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

        // Reduce remaining service demand by actual service received
        if (serviceReceived - remainingSize > EPSILON) {
            if (serviceReceived - remainingSize < 10 * EPSILON) {
                logger.log(Level.WARNING,
                        "Rilevato piccolo errore numerico: serviceReceived {0} > remainingSize {1} per job {2}. Correggo a zero.",
                        new Object[]{serviceReceived, remainingSize, jobId});
                remainingSize = 0.0;
                jobErrorCount++;
            } else {
                logger.log(Level.SEVERE,
                        "Service received {0} exceeds remaining service demand {1} for job {2}",
                        new Object[]{serviceReceived, remainingSize, jobId});
                throw new IllegalStateException("Service received exceeds remaining service demand");
            }
        } else {
            remainingSize -= serviceReceived;
            logger.log(Level.INFO,
                    "Job {0} processed for {1} seconds, remaining service demand: {2}\n",
                    new Object[]{jobId, elapsedTime, remainingSize});
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