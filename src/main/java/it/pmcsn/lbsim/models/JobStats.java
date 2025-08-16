package it.pmcsn.lbsim.models;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JobStats {
    private static final Logger logger = Logger.getLogger(JobStats.class.getName());
    private Double arrivalTime;             // Time when the job arrived
    private Double estimatedDepartureTime;  // Estimated time when the job will depart
    private Job job;                        // The job associated with these stats

    public JobStats(Job job, Double arrivalTime) {
        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        this.job = job;

        if (arrivalTime == null || arrivalTime < 0) {
            logger.log(Level.SEVERE, "Arrival time cannot be null or negative for job {0}", job.getJobId());
            throw new IllegalArgumentException("Arrival time cannot be null or negative");
        }
        this.arrivalTime = arrivalTime;
        this.estimatedDepartureTime = null; // Departure time will be estimated when the job is assigned to a server
    }

    public void estimateDepartureTime(double currentTime) {
        Server assignedServer = job.getAssignedServer(); // Ensure the job has an assigned server
        if (assignedServer == null) {
            logger.log(Level.SEVERE, "Attempted to recalculate remaining service demand without " + "an assigned server. jobId={0}", job.getJobId());
            throw new IllegalStateException("Job must be assigned to a server before recalculating" + " remaining service demand");
        }

        int currentServerSi = assignedServer.getCurrentSi();
        if (currentServerSi <= 0) {
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double remainingSize = job.getRemainingSize();
        double effectiveProcessingRate = assignedServer.getCpuPercentage() * assignedServer.getCpuMultiplier() / currentServerSi;
        estimatedDepartureTime = currentTime + ( remainingSize / effectiveProcessingRate) ;
    }

    // Getters and Setters

    public Double getArrivalTime() {
        return arrivalTime;
    }

    public Job getJob() {
        if (job == null) {
            logger.log(Level.WARNING, "Job not set for job stats");
            throw new IllegalStateException("Job has not been set for this JobStats instance");
        }
        return job;
    }

    public Double getEstimatedDepartureTime() {
        if (estimatedDepartureTime == null) {
            logger.log(Level.WARNING, "Estimated departure time not set for job {0}", job.getJobId());
            throw new IllegalStateException("Estimated departure time has not been calculated yet");
        }
        return estimatedDepartureTime;
    }

}
