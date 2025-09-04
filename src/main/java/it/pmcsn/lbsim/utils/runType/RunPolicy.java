package it.pmcsn.lbsim.utils.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

public interface RunPolicy {



    void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList);
    void updateDepartureStats(int jobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList);
    void updateFinalStats();


}
