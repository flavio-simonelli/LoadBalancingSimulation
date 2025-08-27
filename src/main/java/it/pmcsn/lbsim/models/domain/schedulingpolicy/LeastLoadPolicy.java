package it.pmcsn.lbsim.models.domain.schedulingpolicy;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.List;

// Least Load Scheduling
public class LeastLoadPolicy implements SchedulingPolicy {
    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No available web servers");
        }

        return servers.stream()
                .min((s1, s2) -> Integer.compare(s1.getCurrentSi(), s2.getCurrentSi()))
                .orElseThrow(() -> new IllegalStateException("No available web servers"));
    }
}