package it.pmcsn.lbsim.models.simulation;


import java.util.List;

import java.util.logging.Logger;

public class FutureEventList {
    private static final Logger logger = Logger.getLogger(FutureEventList.class.getName());

    private double nextArrivalTime;                 // Next arrival time for jobs
    private final List<JobStats> jobStats;

    enum Event { DEPARTURE, ARRIVAL }

    public FutureEventList() {
        this.nextArrivalTime = Double.POSITIVE_INFINITY;
        this.jobStats = new java.util.ArrayList<>();
    }


    public Event nextEvent() {
        JobStats bestJob = findNextDepartureJob();
        double nextDepartureTime;
        if (bestJob != null) {
            nextDepartureTime = bestJob.getEstimatedDepartureTime();
        } else {
            nextDepartureTime = Double.POSITIVE_INFINITY;
        }
        return (nextArrivalTime >= nextDepartureTime) ? Event.DEPARTURE : Event.ARRIVAL;
    }


    public double getnextArrivalTime() {
        return this.nextArrivalTime;
    }

    public void setNextArrivalTime(double nextArrivalTime) {
        this.nextArrivalTime = nextArrivalTime;
    }

    public List<JobStats> getJobStats() {
        return jobStats;
    }



    private JobStats findNextDepartureJob() {
    double nextDepartureTime = Double.POSITIVE_INFINITY;
        JobStats bestJob = null;

        for (JobStats stats : jobStats) {
            double depTime = stats.getEstimatedDepartureTime();
            if (depTime < nextDepartureTime) {
                nextDepartureTime = depTime;
                bestJob = stats;
            }
        }
        return bestJob;
    }
    public JobStats nextDepartureJob() {
        return findNextDepartureJob();
    }

    public void addJobStats(JobStats jobStat) {
        jobStats.add(jobStat);
    }
    public void removeJobStats(JobStats jobStat) {jobStats.remove(jobStat);}

    public List<Double> getAllDepartureTimes() {
        List<Double> departureTimes = new java.util.ArrayList<>();
        for (JobStats stats : jobStats) {
            departureTimes.add(stats.getEstimatedDepartureTime());
        }
        return departureTimes;
    }

}
