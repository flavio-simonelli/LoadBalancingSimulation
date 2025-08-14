package it.pmcsn.lbsim.models;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadBalancer {
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());
    private List<Server> webServers; // List of Web Servers
    private Server spikeServer; // Spike Server
    private SlidingWindowResponseTime slidingWindow; // Sliding window for response time
    private final int SImax;
    private final int SImin;
    private final double R0max;
    private final double R0min;

    public LoadBalancer(int initalServerCount, int cpuMultiplierSpike, double cpuPercentageSpike, int windowSize, int SImax, int SImin, double R0max, double R0min) {
        if (initalServerCount <= 0) {
            logger.log(Level.SEVERE,"Initial server count must be greater than zero");
            throw new IllegalArgumentException("Initial server count must be greater than zero");
        }
        if (windowSize <= 0) {
            logger.log(Level.SEVERE,"Sliding window size must be greater than zero");
            throw new IllegalArgumentException("Sliding window size must be greater than zero");
        }
        if (SImin < 0 || SImin >= SImax) {
            logger.log(Level.SEVERE,"SImax must be greater than zero and SImin must be non-negative");
            throw new IllegalArgumentException("SImax must be greater than zero and SImin must be non-negative");
        }
        if (R0min < 0 || R0max <= R0min) {
            logger.log(Level.SEVERE,"RTmax must be greater than RTmin and both must be non-negative");
            throw new IllegalArgumentException("RTmax must be greater than RTmin and both must be non-negative");
        }
        // initialize web server
        webServers = new ArrayList<Server>();
        for (int i=0; i<initalServerCount; i++){
            Server server = new Server(1, 1);
            webServers.add(server);
        }
        // initialize spike server
        spikeServer = new Server(cpuMultiplierSpike, cpuPercentageSpike);
        // initialize sliding window
        slidingWindow = new SlidingWindowResponseTime(windowSize);
        // set SImax and SImin
        this.SImax = SImax;
        this.SImin = SImin;
        // set RTmax and RTmin
        this.R0max = R0max;
        this.R0min = R0min;
    }

    public void departureJob(Job job, double departureTime){
        if (job == null) {
            logger.log(Level.SEVERE,"Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        if ()

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
