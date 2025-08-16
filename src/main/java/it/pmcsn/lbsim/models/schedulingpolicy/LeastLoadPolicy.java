package it.pmcsn.lbsim.models.schedulingpolicy;

import it.pmcsn.lbsim.models.Server;

import java.util.List;

// Least Load Scheduling
public class LeastLoadPolicy implements SchedulingPolicy {
    @Override
    public Server selectServer(List<Server> servers) {
        return servers.stream()
                .min((s1, s2) -> Integer.compare(s1.getCurrentSi(), s2.getCurrentSi()))
                .orElseThrow(() -> new IllegalStateException("No available web servers"));
    }
}