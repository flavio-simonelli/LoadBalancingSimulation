package it.pmcsn.lbsim.models.simulation;


import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.TraceWorkloadGenerator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());

    //welford data fore departures
    private int welfordDepartureN=0;
    private double welfordDepartureAvg=0.0;
    private double welfordDeparturV =0.0;
    private double welfordDepartureS;

    //welford data for arrivals
    private int welfordArrivalN=0;
    private double welfordArrivalAvg=0.0;
    private double welfordArrivalV =0.0;
    private double welfordArrivalS;

    private Double currentTime;                     // Current simulation time
    private final FutureEventList futureEventList; // Future Event List
    private final WorkloadGenerator workload; // Workload generator

    private final LoadBalancer loadBalancer; // System under simulation


    private final CsvAppender csvAppenderServers;
    private final CsvAppender csvAppenderJobs;


    // Constructor
    public Simulator(WorkloadGenerator workloadGenerator,
                     LoadBalancer loadBalancer,
                     CsvAppender csvAppenderServers,
                     CsvAppender csvAppenderJobs) {

        this.currentTime = 0.0;
        this.loadBalancer = loadBalancer;
        this.csvAppenderServers = csvAppenderServers;
        this.csvAppenderJobs = csvAppenderJobs;
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
        int i = 0;
        do  {
            FutureEventList.Event event = this.futureEventList.nextEvent();
            if(event == FutureEventList.Event.ARRIVAL) {
                double nextArrivalTime = this.futureEventList.getnextArrivalTime();
                double elapsedTime = nextArrivalTime - this.currentTime;
                this.currentTime = nextArrivalTime;
                arrivalHandler(elapsedTime, this.currentTime);
                i++;
            } else {
                JobStats nextDepartureJob = this.futureEventList.nextDepartureJob();
                double nextDepartureTime = nextDepartureJob.getEstimatedDepartureTime();
                double elapsedTime = nextDepartureTime - this.currentTime;
                this.currentTime = nextDepartureTime;
                departureHandler(elapsedTime,nextDepartureJob);
            }

        } while ((workload instanceof TraceWorkloadGenerator traceWorkloadGenerator) ? i < traceWorkloadGenerator.getSizeArrival() : this.currentTime < simulationDuration);

        //TODO: gestire nel caso trace driven se non ci sono più arrivi o altri eventi ma il tempo di simulazione non è ancora finito

        // Drain remaining jobs after simulation ends
        this.futureEventList.setNextArrivalTime(Double.POSITIVE_INFINITY); //sicuro da modificare
        while (!this.futureEventList.getJobStats().isEmpty()){
                JobStats nextDepartureJob = futureEventList.nextDepartureJob();
                double nextDepartureTime = nextDepartureJob.getEstimatedDepartureTime();
                double elapsedTime =  nextDepartureTime - this.currentTime;
                this.currentTime = nextDepartureTime;
                departureHandler(elapsedTime,nextDepartureJob);
        }
        // Remove the servers added
        loadBalancer.getWebServers().backToInitialState();


        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsForWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );


        //Welford final calculation
        welfordDepartureS = Math.sqrt(welfordDeparturV/(welfordDepartureN));
        welfordArrivalS = Math.sqrt(welfordArrivalV/(welfordArrivalN));

        //Welford csv
        try {
            CsvAppender welfordCsv = new CsvAppender(Path.of("output/csv/Welford.csv"),"Type","N","Mean","StdDev");
            welfordCsv.writeRow("OriginalSize",String.valueOf(welfordArrivalN),String.valueOf(welfordArrivalAvg),String.valueOf(welfordArrivalS));
            welfordCsv.writeRow("ResponseTime",String.valueOf(welfordDepartureN),String.valueOf(welfordDepartureAvg),String.valueOf(welfordDepartureS));
            welfordCsv.close();
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

        // csv logging


        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsForWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );



        // Generate next arrival time
        this.futureEventList.setNextArrivalTime(this.workload.nextArrival(currentTime));

        //welford for arrivals
        welfordArrivalN++;
        double d = size - this.welfordArrivalAvg;
        welfordArrivalV = welfordArrivalV + d * d * (welfordArrivalN-1)/welfordArrivalN;
        welfordArrivalAvg = welfordArrivalAvg + d/welfordArrivalN;


    }

    private void departureHandler(double elapsedTime, JobStats targetDepartureJobStats) {
        //Process elapsed time for all active jobs
        this.loadBalancer.getWebServers().processJobs(elapsedTime);
        this.loadBalancer.getSpikeServer().processJobs(elapsedTime);

        // Update current time to departure time
        this.currentTime = targetDepartureJobStats.getEstimatedDepartureTime();

        // Process job departure through load balancer
        double responseTime = this.currentTime - targetDepartureJobStats.getArrivalTime();

        this.loadBalancer.completeJob(targetDepartureJobStats.getJob(),this.currentTime, responseTime);

        // Add to the csv for forensics analysis


        this.csvAppenderJobs.writeRow(
                String.valueOf(targetDepartureJobStats.getJob().getJobId()),                                 // job id
                String.valueOf(targetDepartureJobStats.getArrivalTime()),                                    // arrival time
                String.valueOf(currentTime),                                                                 // departure time
                String.valueOf(responseTime),                                                                // response time
                String.valueOf(responseTime - (currentTime - targetDepartureJobStats.getArrivalTime())),                   // response - (departure - arrival)
                String.valueOf(targetDepartureJobStats.getOriginalSize()) ,                                  // original size
                String.valueOf(targetDepartureJobStats.getJob().getAssignedServer().getCpuMultiplier()>1) // processed by spike
        );



        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsForWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );






        this.futureEventList.removeJobStats(targetDepartureJobStats);

        // Recalculate estimated departure times for remaining jobs
        for (JobStats js : this.futureEventList.getJobStats()) {
            js.estimateDepartureTime(this.currentTime);
        }

        //Welford for departures
        welfordDepartureN++;
        double d = responseTime - this.welfordDepartureAvg;
        welfordDeparturV = welfordDeparturV + d * d * (welfordDepartureN-1)/welfordDepartureN;
        welfordDepartureAvg = welfordDepartureAvg + d/welfordDepartureN;
    }
}