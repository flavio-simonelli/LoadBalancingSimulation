package it.pmcsn.lbsim.models.simulation;

import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.Job;

import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: alcuni job hanno arrival time = departure time, lo si osserva dal csv.
// TODO: come gestiamo job diversi con lo stesso istante di arrivo?
public class JobStats {

    // Constants
    private static final Logger logger = Logger.getLogger(JobStats.class.getName());

    // Instance variables
    private final Double arrivalTime;
    private Double estimatedDepartureTime;        // Estimated time when the job will depart according to the assigned server state
    private  final Double originalSize;           // generated size of the job
    private final Job job;                        // The job associated with these stats

    // Constructor
    public JobStats(Job job, Double arrivalTime, Double originalSize) {
        if (job == null) {
            logger.log(Level.SEVERE, "Job cannot be null");
            throw new IllegalArgumentException("Job cannot be null");
        }
        this.job = job;

        if (arrivalTime == null || arrivalTime < 0) {
            logger.log(Level.SEVERE,
                    "Arrival time cannot be null or negative for job {0}",
                    job.getJobId());
            throw new IllegalArgumentException("Arrival time cannot be null or negative");
        }
        this.arrivalTime = arrivalTime;
        this.estimatedDepartureTime = null;
        this.originalSize = originalSize;
    }

    // Methods
    public void estimateDepartureTime(double currentTime) {
        // Ensure the job has an assigned server
        Server assignedServer = job.getAssignedServer();
        if (assignedServer == null) {
            logger.log(Level.SEVERE,
                    "Attempted to recalculate remaining service demand without an assigned server. jobId={0}",
                    job.getJobId());
            throw new IllegalStateException("Job must be assigned to a server before recalculating remaining service demand");
        }

        if (assignedServer.getCurrentSI() <= 0) {
            logger.log(Level.SEVERE,
                    "Current server SI is zero or negative for job {0}",
                    job.getJobId());
            throw new IllegalStateException("Server load cannot be zero or negative");
        }

        // In processor sharing, each job gets 1/n of the CPU time
        double effectiveProcessingRate = assignedServer.getCpuPercentage() *
                assignedServer.getCpuMultiplier() /
                assignedServer.getCurrentSI();

        estimatedDepartureTime = currentTime + (job.getRemainingSize() / effectiveProcessingRate);
    }

    // Getters and Setters
    public Double getArrivalTime() {
        return arrivalTime;
    }

    public Job getJob() {
        if (job == null) {
            logger.log(Level.SEVERE, "Job not set for job stats");
            throw new IllegalStateException("Job has not been set for this JobStats instance");
        }
        return job;
    }

    public Double getEstimatedDepartureTime() {
        if (estimatedDepartureTime == null) {
            logger.log(Level.SEVERE,
                    "Estimated departure time not set for job {0}",
                    job.getJobId());
            throw new IllegalStateException("Estimated departure time has not been calculated yet");
        }
        return estimatedDepartureTime;
    }

    public Double getOriginalSize() {
        return this.originalSize;
    }
}