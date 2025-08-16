package it.pmcsn.lbsim.models.schedulingpolicy;

import it.pmcsn.lbsim.models.Server;

import java.util.List;

// Round Robin Scheduling
public class RoundRobinPolicy implements SchedulingPolicy {
    private int index = 0;

    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No available web servers");
        }
        index = index % servers.size();
        Server server = servers.get(index);
        index = (index + 1) % servers.size();
        return server;
    }
}
