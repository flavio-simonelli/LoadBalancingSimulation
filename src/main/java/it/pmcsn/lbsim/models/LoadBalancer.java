package it.pmcsn.lbsim.models;

import it.pmcsn.lbsim.libs.csv.CsvAppender;
import it.pmcsn.lbsim.models.schedulingpolicy.LeastLoadPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.RoundRobinPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingPolicy;
import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.io.IOException;
import java.nio.file.Path;
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
    private final int SImin;
    private final double R0max;
    private final double R0min;
    private final List<Server> removingServers;   //utilizzata per smaltire i job di un server web che sta per essere chiuso a seguito di uno scale in
    private final List<Server> siCooldownServers;  //utilizzata per il meccanismo di raffreddamento di un web server nel caso in cui precedentemente ha superato il SIMax

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
        webServers = new ArrayList<Server>();
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

        // Set SImax and SImin
        this.SImax = SImax;
        this.SImin = SImin;

        // Set RTmax and RTmin
        this.R0max = R0max;
        this.R0min = R0min;

        // Initialize scaling parameters
        this.horizontalScalingCoolDown = horizontalScalingCoolDown;
        this.lastHorizontalScalingTime = Double.NEGATIVE_INFINITY;
        this.removingServers = new ArrayList<>();
        this.siCooldownServers = new ArrayList<>();
    }

    // Methods
    public void departureJob(JobStats jobStats, Job job, double responseTime, double currentTime) throws IOException {
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

        if ( removingServers.contains(server)   && server.getCurrentSi() == 0) {
            removingServers.remove(server);
            logger.log(Level.INFO, "Removing server has completed all jobs and has been removed.");
        }


        // Update the sliding window with the response time
        slidingWindow.add(responseTime);

            // Add to the csv for forensics analysis
        CsvAppender.getInstance(Path.of("target/Jobs.csv")).writeRow(
                    String.valueOf(job.getJobId()),
                    String.valueOf(jobStats.getArrivalTime()),
                    String.valueOf(currentTime),
                    String.valueOf(responseTime),
                    String.valueOf(job.getAssignedServer().getCurrentSi() >= 10)
        );



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

//        if (siCooldownServers.contains(server) && server.getCurrentSi() < SImin) {
//            siCooldownServers.remove(server);
//            webServers.add(server);
//        }
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
        logger.log(Level.INFO, "Scaled out a Web Server. Total Web Servers: \n" + webServers.size());
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
        if (selectedServer.getCurrentSi() >= SImax) {
            logger.log(Level.INFO,"Selected server has reached SImax (" + SImax + "). Job assigned to Spike.\n");
//            siCooldownServers.add(selectedServer);
//            webServers.remove(selectedServer);

            spikeServer.addJob(job);
            job.assignServer(spikeServer);
            return;
        }


        // TODO: non viene usato SImin, implementare l'uso della policy con SImin

        // Assign the job to the selected server
        selectedServer.addJob(job);
        job.assignServer(selectedServer);
        logger.log(Level.INFO,"Assigned job to Web Server. Current load: \n" + selectedServer.getCurrentSi());
    }
}