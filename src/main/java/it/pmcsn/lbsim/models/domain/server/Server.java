package it.pmcsn.lbsim.models.domain.server;

import it.pmcsn.lbsim.models.domain.Job;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final double EPSILON = 1e-9;

    private final int id;
    private final List<Job> activeJobs;         // List of jobs currently being processed by this server
    private final double cpuMultiplier;            // 1 for WebServer, 2 or 3 for SpikeServer
    private final double cpuPercentage;         // WebServer is 1.0 and SpikeServer is 0.4 or 0.8

    public Server(double cpuMultiplier, double cpuPercentage, int id) {
        this.id = id;
        this.cpuMultiplier = cpuMultiplier;
        this.cpuPercentage = cpuPercentage;
        this.activeJobs = new java.util.ArrayList<>();
    }

    // Getters
    public int getId() { return this.id; }
    public List<Job> getActiveJobs() { return this.activeJobs; }
    public double getCpuMultiplier() { return this.cpuMultiplier; }
    public double getCpuPercentage() { return this.cpuPercentage; }
    public int getCurrentSI() { return this.activeJobs.size(); }

    public void addJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        activeJobs.add(job);
    }

    public void removeJob(Job job) {
        if (job == null) {
            logger.log(Level.SEVERE, "Attempted to remove a null job from the server");
            throw new IllegalArgumentException("Job cannot be null");
        }
        if (!activeJobs.contains(job)) {
            logger.log(Level.WARNING, "Attempted to remove a job that is not present in the server's active job list. jobId={0}", job.getJobId());
            throw new IllegalArgumentException("Job not found in the server's active job list");
        }
        if (job.getRemainingSize()> EPSILON ||job.getRemainingSize()< -EPSILON ) {
            logger.log(Level.WARNING, "Attempted to remove a job that is not yet completed. jobId={0}, remainingSize={1}", new Object[]{job.getJobId(), job.getRemainingSize()});
            throw new IllegalStateException("Cannot remove a job that is not yet completed");
        }
        activeJobs.remove(job);
    }

    public void processJobs(double timeInterval) {
        if (timeInterval < 0) {
            logger.log(Level.SEVERE, "Attempted to process jobs with a negative time interval: {0}", timeInterval);
            throw new IllegalArgumentException("Time interval cannot be negative");
        }
        if (activeJobs.isEmpty()) {
            return; // No jobs to process
        }

        double effectiveProcessingRate = (cpuPercentage * cpuMultiplier) / activeJobs.size();
        double amountToProcess = effectiveProcessingRate * timeInterval;

        for (Job job : new java.util.ArrayList<>(activeJobs)) {
            job.process(amountToProcess);
            if (job.getRemainingSize() <= 0) {
                logger.log(Level.SEVERE, "Job {0} completed and removed from server", job.getJobId()); //TODO: metti in fine il log
            }
        }
    }

}


