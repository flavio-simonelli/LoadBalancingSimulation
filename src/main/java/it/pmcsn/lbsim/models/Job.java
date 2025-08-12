package it.pmcsn.lbsim.models;

public class Job {
    private Server assignedServer; // the server where the job is sent
    private double arrivalTime; // the time the job arrives in the system
    private double departureTime; // the time the job leaves the system
    private double remainingServiceDemand; // the remaining service demand of the job

    public Job(double arrivalTime, double size, Server assignedServer) {
        this.assignedServer = assignedServer; // server where the job is assigned
        this.arrivalTime = arrivalTime; // time of arrival of the job
        this.remainingServiceDemand = size; // remaining size of the job
        this.departureTime = arrivalTime + remainingServiceTime(); // departure time is estimated based on the arrival time, the service demand and the current number of job in the server
    }

    public double departureTime() {
        return departureTime;
    }

    private  double remainingServiceTime() {
        return remainingServiceDemand * (assignedServer.getCurrentSi() + 1);
    }

    public void updateRemainingServiceDemand(double elapsedTime) {
        this.remainingServiceDemand = this.remainingServiceDemand - (elapsedTime / ( (double) 1 / assignedServer.getCurrentSi()));
    }

}