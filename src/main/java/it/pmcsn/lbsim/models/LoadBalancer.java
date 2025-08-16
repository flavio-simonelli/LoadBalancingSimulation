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
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());
    private final List<Server> webServers; // List of Web Servers
    private final Server spikeServer; // Spike Server
    private Server removingServer;
    private final SlidingWindowResponseTime slidingWindow; // Sliding window for response time
    private final SchedulingPolicy schedulingPolicy; // Scheduling policy to use for job assignment
    private final int SImax;
    private final int SImin;
    private final double R0max;
    private final double R0min;

    public LoadBalancer(int initalServerCount, int cpuMultiplierSpike, double cpuPercentageSpike, int windowSize, int SImax, int SImin, double R0max, double R0min, SchedulingType schedulingType) {
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
        // initialize scheduling policy
        switch (schedulingType) {
            case LEAST_LOAD:
                this.schedulingPolicy = new LeastLoadPolicy();
                break;
            case ROUND_ROBIN:
                this.schedulingPolicy = new RoundRobinPolicy();
                break;
            default:
                logger.log(Level.SEVERE,"Unsupported scheduling type: " + schedulingType);
                throw new IllegalArgumentException("Unsupported scheduling type: " + schedulingType);
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

    public void departureJob(Job job, double responseTime){
        if (job == null) {
            logger.log(Level.SEVERE,"Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        if (responseTime <= 0.0) {
            logger.log(Level.SEVERE,"Departure time must be non-negative");
            throw new IllegalArgumentException("Departure time must be non-negative");
        }
        // Remove the job from the server it was assigned to
        Server server = job.getAssignedServer();
        if (server == null) {
            logger.log(Level.SEVERE,"Job is not assigned to any server");
            throw new IllegalStateException("Job is not assigned to any server");
        }
        server.removeJob(job);
        // Update the sliding window with the response time
        slidingWindow.add(responseTime);
        // Check if the server needs to be scaled in/out (horizontal scaling)
        double mean = slidingWindow.getAverage();
        if (mean > R0max){
            // Scale in the server if the average response time exceeds R0max
            scaleInWebServer();
        } else if (mean < R0min) {
            // Scale out the server if the average response time is below R0min
            scaleOutWebServer();
        }
    }

    private void scaleInWebServer(){
        // add web server to the list of web servers
        webServers.add(new Server(1, 1));
        // Log the scaling in action
        logger.log(Level.WARNING, "Scaled in a Web Server. Total Web Servers: " + webServers.size());
    }

    private void scaleOutWebServer() {
        // Remove the last web server from the list of web servers
        if (webServers.size() > 1) {
            // TODO: ATTENZIONE, DOBBIAMO ELIMINARLO DALLA LISTA DEI SERVER SU CUI POSSIAMO SCHEDULARE, MA DOBBIAMO CONSIDERARE CHE HA ANCORA DEI JOB IN ESECUZIONE E DOBBIAMO PRENDERE COMUNQUE I SUOI RESPONSE TIME
            webServers.remove(webServers.size() - 1);
            // Log the scaling out action
            logger.log(Level.WARNING, "Scaled out a Web Server. Total Web Servers: " + webServers.size());
        } else {
            logger.log(Level.WARNING, "Cannot scale out. At least one Web Server must remain.");
        }
    }

    public void jobAssignment(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        // find the server to assign the job to
        Server selectedServer = schedulingPolicy.selectServer(webServers);
        if (selectedServer == null) {
            logger.log(Level.SEVERE, "No available web servers to assign the job");
            throw new IllegalStateException("No available web servers to assign the job");
        } else if (selectedServer.getCurrentSi() >= SImax) {
            logger.log(Level.INFO, "Selected server has reached SImax (" + SImax + "). Job assigned to Spike.");
            spikeServer.addJob(job);
            job.assignServer(spikeServer);
            return;
        } //TODO: non viene usato SImin, implementare l'uso della policy con SImin
        // assign the job to the selected server
        selectedServer.addJob(job);
        job.assignServer(selectedServer);
        logger.log(Level.INFO, "Assigned job to Web Server. Current load: " + selectedServer.getCurrentSi());
    }


}
