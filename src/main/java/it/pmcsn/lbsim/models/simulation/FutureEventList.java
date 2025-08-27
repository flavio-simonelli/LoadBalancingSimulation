package it.pmcsn.lbsim.models.simulation;

import java.util.List;

public class FutureEventList {
    private static final Logger logger = Logger.getLogger(FutureEventList.class.getName());

    private double nextArrivalTime;                 // Next arrival time for jobs
    private final List<JobStats> jobStats;          // List of jobsStats

    public FutureEventList() {
        this.nextArrivalTime = Double.POSITIVE_INFINITY;
        this.jobStats = new java.util.ArrayList<>();
    }

    public double getNextArrivalTime() {
        return nextArrivalTime;
    }

    public void setNextArrivalTime(double nextArrivalTime) {
        this.nextArrivalTime = nextArrivalTime;
    }

    public List<JobStats> getJobStats() {
        return jobStats;
    }

    public void
}
