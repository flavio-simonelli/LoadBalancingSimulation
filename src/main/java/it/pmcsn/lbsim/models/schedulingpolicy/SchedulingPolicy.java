package it.pmcsn.lbsim.models.schedulingpolicy;

import it.pmcsn.lbsim.models.Server;

import java.util.List;

public interface SchedulingPolicy {
    Server selectServer(List<Server> servers);
}
