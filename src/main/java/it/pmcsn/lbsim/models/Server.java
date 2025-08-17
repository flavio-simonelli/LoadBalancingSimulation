package it.pmcsn.lbsim.models;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Server {

    // Constants
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    // Instance variables
    private final List<Job> activeJobs;         // List of jobs currently being processed by this server
    private final int cpuMultiplier;            // 1 for WebServer, 2 or 3 for SpikeServer
    private final double cpuPercentage;         // WebServer is 1.0 and SpikeServer is 0.4 or 0.8


    // Constructor
    public Server(int cpuMultiplier, double cpuPercentage) {
        this.cpuMultiplier = cpuMultiplier;
        this.cpuPercentage = cpuPercentage;
        this.activeJobs = new java.util.ArrayList<>();

    }

    // Methods
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
        activeJobs.remove(job);
    }

    public int getCurrentSi() {
        return activeJobs.size();
    }

    // Getters
    public int getCpuMultiplier() {
        return cpuMultiplier;
    }

    public double getCpuPercentage() {
        return cpuPercentage;
    }

}