package it.pmcsn.lbsim.models.simulation;


import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.TraceWorkloadGenerator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.TimeMediateWelford;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());

    private final WelfordSimple departureStats = new WelfordSimple();
    private final WelfordSimple arrivalStats = new WelfordSimple();
    private final TimeMediateWelford meanNumberJobs = new TimeMediateWelford(120.0);

    private Double currentTime;                     // Current simulation time
    private final FutureEventList futureEventList; // Future Event List
    private final WorkloadGenerator workload; // Workload generator

    private final LoadBalancer loadBalancer; // System under simulation


    private final CsvAppender welfordCsv;
    private final CsvAppender jobLogCsv;
    private final CsvAppender serverLogCsv;


    // Constructor
    public Simulator(WorkloadGenerator workloadGenerator,
                     LoadBalancer loadBalancer,
                     CsvAppender csvWelford,
                     CsvAppender jobLogCsv,
                     CsvAppender serverLogCsv) {

        this.currentTime = 0.0;
        this.loadBalancer = loadBalancer;
        this.welfordCsv = csvWelford;
        this.jobLogCsv = jobLogCsv;
        this.serverLogCsv = serverLogCsv;
        this.workload = workloadGenerator;
        this.futureEventList = new FutureEventList();
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
                arrivalHandler(elapsedTime, this.currentTime);
                this.futureEventList.setNextArrivalTime(this.workload.nextArrival(currentTime));
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
        IntervalEstimation intervalEstimation = new IntervalEstimation(0.95);
        try {
            welfordCsv.writeRow("OriginalSize",String.valueOf(arrivalStats.getI()),String.valueOf(arrivalStats.getAvg()),String.valueOf(arrivalStats.getStandardVariation()), String.valueOf(arrivalStats.getVariance()), String.valueOf(intervalEstimation.SemiIntervalEstimation(arrivalStats.getStandardVariation(), arrivalStats.getI())));
            welfordCsv.writeRow("ResponseTime",String.valueOf(departureStats.getI()),String.valueOf(departureStats.getAvg()),String.valueOf(departureStats.getStandardVariation()), String.valueOf(departureStats.getVariance()), String.valueOf(intervalEstimation.SemiIntervalEstimation(departureStats.getStandardVariation(), departureStats.getI())));
            welfordCsv.writeRow("MeanNumberJobs",String.valueOf(meanNumberJobs.getI()),String.valueOf(meanNumberJobs.getAvg()),String.valueOf(meanNumberJobs.getStandardVariation()), String.valueOf(meanNumberJobs.getVariance()), String.valueOf(intervalEstimation.SemiIntervalEstimation(meanNumberJobs.getStandardVariation(), meanNumberJobs.getI())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        try{
            for (var server : loadBalancer.getWebServers().getWebServers()) {
                serverLogCsv.writeRow(
                        String.valueOf(this.currentTime),
                        String.valueOf(server.getId()),
                        String.valueOf(loadBalancer.getWebServers().isRemovingServer(server)),
                        String.valueOf(server.getCurrentSI())
                );
            }
            serverLogCsv.writeRow(
                    String.valueOf(this.currentTime),
                    String.valueOf(loadBalancer.getSpikeServer().getId()),
                    "false",
                    String.valueOf(loadBalancer.getSpikeServer().getCurrentSI())
            );
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        //welford for arrivals
        arrivalStats.iteration(size);
        meanNumberJobs.iteration(loadBalancer.getCurrentJobCount(), this.currentTime);
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
        try {
            jobLogCsv.writeRow(
                    String.valueOf(targetDepartureJobStats.getJob().getJobId()),
                    String.format("%.6f", targetDepartureJobStats.getArrivalTime()),
                    String.format("%.6f", this.currentTime),
                    String.format("%.6f", responseTime),
                    String.format("%.6f", targetDepartureJobStats.getOriginalSize()),
                    String.valueOf(targetDepartureJobStats.getJob().getAssignedServer().getId())
            );
            for (var server : loadBalancer.getWebServers().getWebServers()) {
                serverLogCsv.writeRow(
                        String.valueOf(this.currentTime),
                        String.valueOf(server.getId()),
                        String.valueOf(loadBalancer.getWebServers().isRemovingServer(server)),
                        String.valueOf(server.getCurrentSI())
                );
            }
            serverLogCsv.writeRow(
                    String.valueOf(this.currentTime),
                    String.valueOf(loadBalancer.getSpikeServer().getId()),
                    "false",
                    String.valueOf(loadBalancer.getSpikeServer().getCurrentSI())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // welford for departures
        departureStats.iteration(responseTime);
        meanNumberJobs.iteration(loadBalancer.getCurrentJobCount(), this.currentTime);
    }
}