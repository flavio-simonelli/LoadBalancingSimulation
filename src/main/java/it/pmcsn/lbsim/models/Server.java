package it.pmcsn.lbsim.models;

import java.util.List;

public class Server {
    private int id;
    private double serviceRate; // the rate at which the server processes jobs
    private double currentUtilization; // the fraction of time the server is busy
    private double idleTime; // the total time the server is idle
    private double busyTime; // the total time the server is busy
    private int maxJob; // the maximum number of job the server can do
    private int currentSi; // the service intensity, which is the average number of jobs served per time unit
    private List<Job> jobs; // the list of jobs currently assigned to the server


    public Server(double serviceRate) {
        this.serviceRate = serviceRate;
        setUtilization(0.0);
        this.idleTime = 0.0;
        this.busyTime = 0.0;
    }

    public Server() {
    }


    public double getServiceRate() {
        return serviceRate;
    }

    public void setServiceRate(double serviceRate) {
        this.serviceRate = serviceRate;
    }

    public double getUtilization() {
        return this.currentUtilization;
    }

    public void setUtilization(double utilization) {
        this.currentUtilization = utilization;
    }

    public double getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(double idleTime) {
        this.idleTime = idleTime;
    }

    public double getBusyTime() {
        return busyTime;
    }

    public void setBusyTime(double busyTime) {
        this.busyTime = busyTime;
    }
    public int getMaxJob() {
        return maxJob;
    }
    public void setMaxJob(int maxJob) {
        this.maxJob = maxJob;
    }
    public int getCurrentSi() {
        return currentSi;
    }
    public void setCurrentSi(int si) {
        this.currentSi = si;
    }
    public List<Job> getJobs() {
        return jobs;
    }
    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }
}
