package it.pmcsn.lbsim.models.domain.server;

import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicy;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerPool {
    public final static Logger logger = Logger.getLogger(ServerPool.class.getName());
    private final double cpuMultiplier;
    private final int initialServerCount;
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
        this.initialServerCount = initialServerCount;
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
            logger.log(Level.WARNING,"Cannot scale in. At least one Web Server must remain.\n");
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
                result.add(idToSI.getOrDefault(id, null));
            }
            res = result;
        }
        return res;
    }

    public List<Double> getRemianingSizeForAllJobs(){
        List<Double> res = new ArrayList<>();
        // Unisci tutti i server attivi e in draining
        List<Server> allServers = new ArrayList<>();
        allServers.addAll(webServers);
        allServers.addAll(removingServers);
        for (Server server : allServers) {
            for (Job job : server.getActiveJobs()) {
                res.add(job.getRemainingSize());
            }
        }
        return res;
    }

    public void backToInitialState() {
        // 1) Pulisci i server in draining
        for (Server s : new ArrayList<>(removingServers)) {
            removingServers.remove(s);
            idAllocator.release(s.getId());
            logger.log(Level.INFO, "BackToInitialState: removed draining server id={0}", s.getId());
        }

        // 2) Rimuovi server attivi se ce ne sono troppi
        while (webServers.size() > initialServerCount) {
            Server toRemove = removalPolicy.chooseServerToRemove(webServers);
            if (toRemove == null && !webServers.isEmpty()) {
                toRemove = webServers.get(webServers.size() - 1);
            }
            if (toRemove == null) break;

            webServers.remove(toRemove);
            idAllocator.release(toRemove.getId());
            logger.log(Level.INFO, "BackToInitialState: removed extra server id={0}", toRemove.getId());
        }

        // 3) Aggiungi nuovi server se ce ne sono troppo pochi
        while (webServers.size() < initialServerCount) {
            Server newServer = new Server(cpuMultiplier, 1, idAllocator.allocate());
            webServers.add(newServer);
            logger.log(Level.INFO, "BackToInitialState: added new server id={0}", newServer.getId());
        }
    }

    public boolean isRemovingServer(Server server){
        return removingServers.contains(server);
    }


}
