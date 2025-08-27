package it.pmcsn.lbsim.models.domain.removalPolicy;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.List;

public interface RemovalPolicy {
    Server chooseServerToRemove(List<Server> activeServers);
}
