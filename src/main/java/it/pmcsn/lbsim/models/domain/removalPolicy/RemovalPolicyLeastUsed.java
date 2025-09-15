package it.pmcsn.lbsim.models.domain.removalPolicy;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.Comparator;
import java.util.List;

public class RemovalPolicyLeastUsed implements RemovalPolicy{

    @Override
    public Server chooseServerToRemove(List<Server> activeServers) {
        if (activeServers == null || activeServers.isEmpty()) {
            throw new IllegalArgumentException("Active servers list cannot be null or empty");
        }

        return activeServers.stream()
                .min(Comparator.comparingInt(Server::getCurrentSI))
                .orElseThrow(() -> new IllegalStateException("No servers available to remove"));
    }
}
