package it.pmcsn.lbsim.models.domain;

import it.pmcsn.lbsim.models.domain.scaling.horizontalscaler.HorizontalScaler;
import it.pmcsn.lbsim.models.domain.scaling.spikerouter.SpikeRouter;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingPolicy;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.server.ServerPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoadBalancer {
    private static final double EPSILON = 1e-10;
    // Constants
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());

    // Instance variables
    private final ServerPool webServers;                 // List of web servers
    private final Server spikeServer;                           // Spike Server
    private final SchedulingPolicy schedulingPolicy;            // Scheduling policy to use for job assignment
    private final HorizontalScaler horizontalScaler;           // Horizontal scaler
    private final SpikeRouter spikeRouter;                   // Spike router

    public LoadBalancer(ServerPool pool,
                        Server spikeServer,
                        SchedulingPolicy schedulingPolicy,
                        SpikeRouter spikeRouter,
                        HorizontalScaler horizontalScaler) {
        this.webServers = pool;
        this.spikeServer = spikeServer;
        this.schedulingPolicy = schedulingPolicy;
        this.spikeRouter = spikeRouter;
        this.horizontalScaler = horizontalScaler;
    }

    public ServerPool getWebServers() {
        return webServers;
    }

    public Server getSpikeServer() {
        return spikeServer;
    }

    public void assignJob(Job job, double currentTime) {
        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        // Find the server to assign the job to
        Server selectedServer = schedulingPolicy.selectServer(webServers.getWebServers());
        if (selectedServer == null) {
            logger.log(Level.SEVERE, "No available web servers to assign the job");
            throw new IllegalStateException("No available web servers to assign the job");
        }
        // Decide whether to route to spike or assign to chosen server
        SpikeRouter.Action action = spikeRouter.decide(selectedServer, currentTime);
        if (action == SpikeRouter.Action.ROUTE_TO_SPIKE) {
            spikeServer.addJob(job);
            job.assignServer(spikeServer);
            logger.log(Level.FINE,"Assigned job to Spike Server. Current load: " + spikeServer.getCurrentSI() + "\n");
        } else {
            selectedServer.addJob(job);
            job.assignServer(selectedServer);
            logger.log(Level.FINE,"Assigned job to Web Server. Current load: " + selectedServer.getCurrentSI() + "\n");
        }
    }

    public void completeJob(Job job, double currentTime, double responseTime) {

        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        if (responseTime <= 0.0) {
            logger.log(Level.SEVERE, "Completion time must be non-negative");
            throw new IllegalArgumentException("Completion time must be non-negative");
        }
        if (job.getRemainingSize() > EPSILON) {
            logger.log(Level.SEVERE, "Job is not yet completed. Remaining size: " + job.getRemainingSize());
            throw new IllegalStateException("Job is not yet completed");
        }
        if (currentTime < 0.0) {
            logger.log(Level.SEVERE, "Current time must be non-negative");
            throw new IllegalArgumentException("Current time must be non-negative");
        }
        // complete the job on the assigned server
        this.webServers.completeJob(job);
        // notify the horizontal scaler
        HorizontalScaler.Action action = this.horizontalScaler.notifyJobDeparture(responseTime, currentTime);
        switch (action) {
            case SCALE_OUT -> {
                if (this.webServers.requestScaleOut()) {
                    this.horizontalScaler.setLastActionAt(currentTime);
                }
            }
            case SCALE_IN -> {
                if(this.webServers.requestScaleIn()) {
                    this.horizontalScaler.setLastActionAt(currentTime);
                }
            }
            case NONE -> {
                // No action needed
            }
        }
    }

    public int getWebServerCount() {
        return webServers.getWebServerCount();
    }

    public int getSpikeServerJobCount() {
        return spikeServer.getCurrentSI();
    }

    public String getJobCountsForWebServer(){
        List<Integer> jobs = webServers.getJobsCountForServer();
        return jobs.stream()
                .map(v -> v == null ? "null" : v.toString())
                .collect(Collectors.joining(",", "[", "]"));
    }
}