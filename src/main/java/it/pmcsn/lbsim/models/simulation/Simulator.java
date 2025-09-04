package it.pmcsn.lbsim.models.simulation;


import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.runType.RunPolicy;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());
    private Double currentTime;                     // Current simulation time
    private final FutureEventList futureEventList; // Future Event List
    private final WorkloadGenerator workload; // Workload generator
    private final LoadBalancer loadBalancer; // System under simulation
    private RunPolicy runPolicy;

    public Simulator(WorkloadGenerator workloadGenerator, LoadBalancer loadBalancer, RunPolicy runPolicy) {
        this.currentTime = 0.0;
        this.loadBalancer = loadBalancer;
        this.runPolicy = runPolicy;
        this.workload = workloadGenerator;
        this.futureEventList = new FutureEventList();
    }

    public void run(int numJobs) {

    }

    public void run(double simulationDuration) {
        if (simulationDuration <= 0.0) {
            logger.log(Level.SEVERE, "Simulation Duration must be greater than zero");
            throw new IllegalArgumentException("Simulation duration must be greater than zero");
        }
        this.futureEventList.setNextArrivalTime(this.workload.nextArrival(currentTime));
        // Main simulation loop - process events until simulation duration
        while (this.futureEventList.getnextArrivalTime() <= simulationDuration) {
            FutureEventList.Event event = this.futureEventList.nextEvent();
            if(event == FutureEventList.Event.ARRIVAL) {
                double nextArrivalTime = this.futureEventList.getnextArrivalTime();
                if (nextArrivalTime == Double.POSITIVE_INFINITY) {
                    break; // No more events to process
                }
                double elapsedTime = nextArrivalTime - this.currentTime;
                this.currentTime = nextArrivalTime;
                this.futureEventList.setNextArrivalTime(this.workload.nextArrival(currentTime));

                arrivalHandler(elapsedTime, this.currentTime);
            } else {
                JobStats nextDepartureJob = this.futureEventList.nextDepartureJob();
                double nextDepartureTime = nextDepartureJob.getEstimatedDepartureTime();
                if (nextDepartureTime == Double.POSITIVE_INFINITY) {
                    break; // No more events to process
                }
                double elapsedTime = nextDepartureTime - this.currentTime;
                this.currentTime = nextDepartureTime;



                departureHandler(elapsedTime,nextDepartureJob);
            }
        }

        // Drain remaining jobs after simulation ends
        while ( this.futureEventList.nextDepartureJob() != null) {
                JobStats nextDepartureJob = this.futureEventList.nextDepartureJob();
                double nextDepartureTime = nextDepartureJob.getEstimatedDepartureTime();
                double elapsedTime =  nextDepartureTime - this.currentTime;
                this.currentTime = nextDepartureTime;
                departureHandler(elapsedTime,nextDepartureJob);
        }
        // Remove the servers added
        loadBalancer.getWebServers().backToInitialState();
        //Welford csv
      /*  try {
            welfordCsv.writeRow("OriginalSize", String.valueOf(arrivalStats.getI()), String.valueOf(arrivalStats.getAvg()), String.valueOf(arrivalStats.getStandardVariation()), String.valueOf(arrivalStats.getVariance()), String.valueOf(intervalEstimation.semiIntervalEstimation(arrivalStats.getStandardVariation(), arrivalStats.getI())));
            welfordCsv.writeRow("ResponseTime", String.valueOf(departureStats.getI()), String.valueOf(departureStats.getAvg()), String.valueOf(departureStats.getStandardVariation()), String.valueOf(departureStats.getVariance()), String.valueOf(intervalEstimation.semiIntervalEstimation(departureStats.getStandardVariation(), departureStats.getI())));
            welfordCsv.writeRow("MeanNumberJobs", String.valueOf(meanNumberJobs.getI()), String.valueOf(meanNumberJobs.getAvg()), String.valueOf(meanNumberJobs.getStandardVariation()), String.valueOf(meanNumberJobs.getVariance()), String.valueOf(intervalEstimation.semiIntervalEstimation(meanNumberJobs.getStandardVariation(), meanNumberJobs.getI())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
    }


    private void arrivalHandler(double elapsedTime, double currentTime) {
        // Process elapsed time for all active jobs
        this.loadBalancer.getWebServers().processJobs(elapsedTime);
        this.loadBalancer.getSpikeServer().processJobs(elapsedTime);

        // Create new job
        double size = this.workload.nextJobSize();
        Job newJob = new Job(size);
        // assign job to load balancer
        this.loadBalancer.assignJob(newJob, currentTime);
        JobStats newJobStats = new JobStats(newJob, this.currentTime, size);
        this.futureEventList.addJobStats(newJobStats);

        // Recalculate estimated departure times for all jobs
        for (JobStats jobStat : this.futureEventList.getJobStats()) {
            jobStat.estimateDepartureTime(this.currentTime);
        }

        runPolicy.updateArrivalStats(size, loadBalancer.getCurrentJobCount(), this.currentTime, this.loadBalancer, this.futureEventList);


    }

    private void departureHandler(double elapsedTime, JobStats targetDepartureJobStats) {
        //Process elapsed time for all active jobs
        this.loadBalancer.getWebServers().processJobs(elapsedTime);
        this.loadBalancer.getSpikeServer().processJobs(elapsedTime);

        // Process job departure through load balancer
        double responseTime = this.currentTime - targetDepartureJobStats.getArrivalTime();

        this.loadBalancer.completeJob(targetDepartureJobStats.getJob(),this.currentTime, responseTime);

        // Add to the csv for forensics analysis
        this.futureEventList.removeJobStats(targetDepartureJobStats);

        // Recalculate estimated departure times for remaining jobs
        for (JobStats js : this.futureEventList.getJobStats()) {
            js.estimateDepartureTime(this.currentTime);
        }

        // Log job statistics

        this.runPolicy.updateDepartureStats(loadBalancer.getCurrentJobCount(), this.currentTime, responseTime, targetDepartureJobStats,loadBalancer,futureEventList);
    }


}