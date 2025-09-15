package it.pmcsn.lbsim.models.domain.schedulingpolicy;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.List;

public interface SchedulingPolicy {
    Server selectServer(List<Server> servers);
}
