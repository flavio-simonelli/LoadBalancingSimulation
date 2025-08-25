package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.models.schedulingpolicy.LeastLoadPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.RoundRobinPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadBalancer {

    // Constants
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());

    // Instance variables
    private final List<Server> webServers;                      // List of Web Servers
    private final Server spikeServer;                           // Spike Server
    private final double horizontalScalingCoolDown;
    private double lastHorizontalScalingTime;                   // Last time a horizontal scaling action was performed
    private final SlidingWindowResponseTime slidingWindow;      // Sliding window for response time
    private final SchedulingPolicy schedulingPolicy;            // Scheduling policy to use for job assignment
    private final int SImax;
    private final double R0max;
    private final double R0min;
    private final List<Server> removingServers;                 //utilizzata per smaltire i job di un server web che sta per essere chiuso a seguito di uno scale in


    // Constructor
    public LoadBalancer(int initalServerCount,
                        int cpuMultiplierSpike,
                        double cpuPercentageSpike,
                        int windowSize,
                        int SImax,
                        int SImin,
                        double R0max,
                        double R0min,
                        SchedulingType schedulingType,
                        double horizontalScalingCoolDown) {

        // Input validation
        if (initalServerCount <= 0) {
            logger.log(Level.SEVERE, "Initial server count must be greater than zero");
            throw new IllegalArgumentException("Initial server count must be greater than zero");
        }

        if (windowSize <= 0) {
            logger.log(Level.SEVERE, "Sliding window size must be greater than zero");
            throw new IllegalArgumentException("Sliding window size must be greater than zero");
        }

        if (SImin < 0 || SImin >= SImax) {
            logger.log(Level.SEVERE, "SImax must be greater than zero and SImin must be non-negative");
            throw new IllegalArgumentException("SImax must be greater than zero and SImin must be non-negative");
        }

        if (R0min < 0 || R0max <= R0min) {
            logger.log(Level.SEVERE, "RTmax must be greater than RTmin and both must be non-negative");
            throw new IllegalArgumentException("RTmax must be greater than RTmin and both must be non-negative");
        }

        // Initialize web servers
        webServers = new ArrayList<>();
        for (int i = 0; i < initalServerCount; i++) {
            Server server = new Server(1, 1);
            webServers.add(server);
        }

        // Initialize scheduling policy
        switch (schedulingType) {
            case LEAST_LOAD:
                this.schedulingPolicy = new LeastLoadPolicy();
                break;
            case ROUND_ROBIN:
                this.schedulingPolicy = new RoundRobinPolicy();
                break;
            default:
                logger.log(Level.SEVERE, "Unsupported scheduling type: " + schedulingType);
                throw new IllegalArgumentException("Unsupported scheduling type: " + schedulingType);
        }

        // Initialize spike server
        spikeServer = new Server(cpuMultiplierSpike, cpuPercentageSpike);

        // Initialize sliding window
        slidingWindow = new SlidingWindowResponseTime(windowSize);

        // Set SImax
        this.SImax = SImax;

        // Set RTmax and RTmin
        this.R0max = R0max;
        this.R0min = R0min;

        // Initialize scaling parameters
        this.horizontalScalingCoolDown = horizontalScalingCoolDown;
        this.lastHorizontalScalingTime = Double.NEGATIVE_INFINITY;
        this.removingServers = new ArrayList<>();
    }

    // Methods
    public void departureJob(Job job, double responseTime, double currentTime) {
        // Input validation
        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }

        if (responseTime <= 0.0) {
            logger.log(Level.SEVERE, "Departure time must be non-negative");
            throw new IllegalArgumentException("Departure time must be non-negative");
        }

        // Remove the job from the server it was assigned to
        Server server = job.getAssignedServer();
        if (server == null) {
            logger.log(Level.SEVERE, "Job is not assigned to any server");
            throw new IllegalStateException("Job is not assigned to any server");
        }
        server.removeJob(job);

        if ( removingServers.contains(server) && server.getCurrentSi() == 0) {
            removingServers.remove(server);
            logger.log(Level.INFO, "Removing server has completed all jobs and has been removed. \n");
        }

        // Update the sliding window with the response time
        slidingWindow.add(responseTime);

        // Check if the server needs to be scaled in/out (horizontal scaling)
        double mean = slidingWindow.getAverage();

        if (mean > R0max && (currentTime - lastHorizontalScalingTime) >= horizontalScalingCoolDown) {
            // Update the last horizontal scaling time
            lastHorizontalScalingTime = currentTime;
            // Scale out the server if the average response time exceeds R0max
            scaleOutWebServer();
        } else if (mean < R0min && (currentTime - lastHorizontalScalingTime) >= horizontalScalingCoolDown) {
            // Update the last horizontal scaling time
            lastHorizontalScalingTime = currentTime;
            // Scale in the server if the average response time is below R0min
            scaleInWebServer();
        }
    }

    private void scaleOutWebServer() {
        // check if there are already server in the list of removing server
        if (!removingServers.isEmpty()) {
            Server server = removingServers.getLast();
            removingServers.remove(server);
            webServers.add(server);
        } else {
            // Add web server to the list of web servers
            webServers.add(new Server(1, 1));
        }
        // Log the scaling out action
        logger.log(Level.INFO, "Scaled out a Web Server. Total Web Servers: " + webServers.size() + "\n");
    }

    private void scaleInWebServer() {
        // Remove the last web server from the list of web servers
        if (webServers.size() <= 1) {
            logger.log(Level.WARNING, "Cannot scale in. At least one Web Server must remain.\n");
            return;
        }

        Server server = webServers.getLast();
        webServers.remove(server);
        if (server.getCurrentSi() == 0) {
            logger.log(Level.INFO, "Scaled in immediately. Server had no active jobs.\n");
        } else {
            this.removingServers.add(server);
            logger.log(Level.INFO, "Server is draining. Will be removed when all jobs are done.\n");
        }
    }

    public void jobAssignment(Job job) {
        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }

        // Find the server to assign the job to
        Server selectedServer = schedulingPolicy.selectServer(webServers);
        if (selectedServer == null) {
            logger.log(Level.SEVERE, "No available web servers to assign the job");
            throw new IllegalStateException("No available web servers to assign the job");
        }
        // If policy is "leastUsed" then this scenario represents the case where all servers are saturated
        if (selectedServer.getCurrentSi() >= SImax) {
            logger.log(Level.INFO,"Selected server has reached SImax (\" {0} \"). Job assigned to Spike.\n", SImax);
            spikeServer.addJob(job);
            job.assignServer(spikeServer);
            return;
        }


        // Assign the job to the selected server
        selectedServer.addJob(job);
        job.assignServer(selectedServer);
        logger.log(Level.INFO,"Assigned job to Web Server. Current load: " + selectedServer.getCurrentSi() + "\n");
    }

    public int getWebServerCount() {
        return webServers.size();
    }

    public List<Integer> getJobCountsPerWebServer() {
        List<Integer> jobCounts = new ArrayList<>();
        for (Server server : webServers) {
            jobCounts.add(server.getCurrentSi());
        }
        return jobCounts;
    }

    public int getSpikeServerJobCount() {
        return spikeServer.getCurrentSi();
    }
}