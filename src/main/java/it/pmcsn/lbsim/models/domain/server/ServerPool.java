package it.pmcsn.lbsim.models.domain.server;

import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicy;

import java.util.*;
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
            logger.log(Level.INFO,"Scaled in immediately. Removed server id=" + toRemove.getId());
        } else {
            removingServers.add(toRemove);
            logger.log(Level.INFO,"Server id=" + toRemove.getId() + " draining...");
        }
        return true;
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

    public int getWebServerCount() {
        return webServers.size();
    }



    public List<Integer> getJobsCountForServer() {
        List<Integer> res;
        Map<Integer, Integer> idToSI = new HashMap<>();
        // Unisci tutti i server attivi e in draining
        List<Server> allServers = new ArrayList<>();
        allServers.addAll(webServers);
        allServers.addAll(removingServers);
        // Costruisci la mappa ID -> SI
        for (Server server : allServers) {
            idToSI.put(server.getId(), server.getCurrentSI());
        }
        // Trova min e max id
        if (idToSI.isEmpty()) {
            res = new ArrayList<>();
        } else {
            int maxId = Collections.max(idToSI.keySet());
            List<Integer> result = new ArrayList<>();
            for (int id = 0; id <= maxId; id++) {
                if (idToSI.containsKey(id)) {
                    result.add(idToSI.get(id));
                } else {
                    result.add(null);
                }
            }
            res = result;
        }
        return res;
    }


}
