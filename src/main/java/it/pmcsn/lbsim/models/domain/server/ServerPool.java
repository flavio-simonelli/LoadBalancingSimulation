package it.pmcsn.lbsim.models.domain.server;

import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerPool {
    public final static Logger logger = Logger.getLogger(ServerPool.class.getName());
    private final double cpuMultiplier;
    private final List<Server> webServers;
    private final List<Server> removingServers;
    private final RemovalPolicy removalPolicy;
    private final ServerIdAllocator idAllocator = new ServerIdAllocator();

    public ServerPool(int initialServerCount, double cpuMultiplier, RemovalPolicy removalPolicy) {
        if (removalPolicy == null) throw new IllegalArgumentException("Removal policy cannot be null");
        if (initialServerCount <= 0) throw new IllegalArgumentException("Initial server count must be > 0");
        if (cpuMultiplier <= 0) throw new IllegalArgumentException("CPU Multiplier must be > 0");
        this.removalPolicy = removalPolicy;
        this.cpuMultiplier = cpuMultiplier;
        webServers = new ArrayList<>();
        for (int i = 0; i < initialServerCount; i++) {
            Server server = new Server(cpuMultiplier, 1, idAllocator.allocate());
            webServers.add(server);
        }
        removingServers = new ArrayList<>();
    }

    public List<Server> getWebServers() {
        return webServers;
    }

    // return true if scale-in request accepted, false otherwise
    public boolean requestScaleIn() {
        if (webServers.size() <= 1) {
            logger.log(Level.WARNING,"Cannot scale in. At least one Web Server must remain.");
            return false;
        }
        Server toRemove = removalPolicy.chooseServerToRemove(webServers);
        webServers.remove(toRemove);
        if (toRemove.getCurrentSI() == 0) {
            idAllocator.release(toRemove.getId());
            logger.info("Scaled in immediately. Removed server id=" + toRemove.getId());
            return true;
        } else {
            removingServers.add(toRemove);
            logger.info("Server id=" + toRemove.getId() + " draining...");
            return true;
        }
    }

    // return true if scale-out request accepted, false otherwise
    public boolean requestScaleOut() {
        Server server = new Server(cpuMultiplier, 1, idAllocator.allocate());
        webServers.add(server);
        return true;
    }

    public void completeJob(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }
        Server assignedServer = job.getAssignedServer();
        if (assignedServer == null) {
            throw new IllegalStateException("Job is not assigned to any server");
        }
        assignedServer.removeJob(job);
        if (removingServers.contains(assignedServer) && assignedServer.getActiveJobs().isEmpty()) {
            removingServers.remove(assignedServer);
            idAllocator.release(assignedServer.getId());
        }
    }

    public void processJobs(double timeInterval) {
        if (timeInterval < 0) {
            throw new IllegalArgumentException("Time interval cannot be negative");
        }
        for (Server server : webServers) {
            server.processJobs(timeInterval);
        }
        for (Server server : new ArrayList<>(removingServers)) {
            server.processJobs(timeInterval);
        }
    }


}
