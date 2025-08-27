package it.pmcsn.lbsim.models.domain.schedulingpolicy;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.List;

// Round Robin Scheduling
public class RoundRobinPolicy implements SchedulingPolicy {
    private int index = 0;

    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No available web servers");
        }

        if (index >= servers.size()) {
            index = 0;
        }

        Server server = servers.get(index);
        index = (index + 1) % servers.size();
        return server;
    }
}