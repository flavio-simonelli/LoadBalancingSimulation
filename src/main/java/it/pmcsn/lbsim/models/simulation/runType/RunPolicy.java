package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;

public interface RunPolicy {



    void updateArrivalStats(double time, JobStats newJobStats, LoadBalancer loadBalancer);
    void updateDepartureStats(double currentTime, JobStats departureJob, LoadBalancer loadBalancer, double responseTime);
    void updateFinalStats();
    void closeCsvs();


}
