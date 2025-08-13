package it.pmcsn.lbsim.models;

import java.util.List;

public class LoadBalancer {
    private List<Server> webServers; // List of Web Servers
    private Server spikeServer; // Spike Server
    private SlidingWindowResponseTime slidingWindow; // Sliding window for response time
    private final int SImax;
    private final int SImin;
    private final double RTmax;
    private final double RTmin;

    public LoadBalancer(List<Server> webServers, Server spikeServer, SlidingWindowResponseTime slidingWindow) {
        if (webServers == null || webServers.isEmpty()) {
            throw new IllegalArgumentException("Web servers list cannot be null or empty");
        }
        if (spikeServer == null) {
            throw new IllegalArgumentException("Spike server cannot be null");
        }
        if (slidingWindow == null) {
            throw new IllegalArgumentException("Sliding window cannot be null");
        }

        this.webServers = webServers;
        this.spikeServer = spikeServer;
        this.slidingWindow = slidingWindow;
    }

    public assignJobToServer(Job job, double currentTime) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        // Assign the job to the server with the least load
        leastLoadSchedule(job, currentTime);
    }

    private void leastLoadSchedule(Job job, double currentTime) {
        // Assign the job to the server with the least load
        Server selectedServer = webServers.stream()
                .min((s1, s2) -> Integer.compare(s1.getCurrentSi(), s2.getCurrentSi()))
                .orElseThrow(() -> new IllegalStateException("No available web servers"));

        selectedServer.addJob(job);
        job.assignServer(selectedServer, currentTime);
    }

    // ha la responabilità di fare la scalabilità orizzontale e verticale e anche di scheduling, cioè decidere a quale server dare il job


}
