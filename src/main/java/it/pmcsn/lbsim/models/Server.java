package it.pmcsn.lbsim.models;

import java.util.List;

public class Server {
    private List<Job> activeJobs;       // List of jobs currently being processed by this server
    private final int cpuMultiplier;    // 1 for WebServer, 2 or 3 for SpikeServer
    private final double cpuPercentage; // WebServer is 1.0 and SpikeServer is 0.4 or 0.8

    public Server(int cpuMultiplier, double cpuPercentage) {
        this.cpuMultiplier = cpuMultiplier;
        this.cpuPercentage = cpuPercentage;
        this.activeJobs = new java.util.ArrayList<>();
    }

    public void addJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        activeJobs.add(job);
    }

    public void removeJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        activeJobs.remove(job);
    }

    public int getCurrentSi(){
        return activeJobs.size();
    }

    public int getCpuMultiplier() {
        return cpuMultiplier;
    }

    public double getCpuPercentage() {
        return cpuPercentage;
    }
}
