package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;

public interface RunPolicy {



    void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList);
    void updateDepartureStats(int jobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList);
    void updateFinalStats();
    void closeCsvs();


}
