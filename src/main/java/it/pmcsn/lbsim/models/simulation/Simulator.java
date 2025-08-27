package it.pmcsn.lbsim.models.simulation;


import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());

    private Double currentTime;                     // Current simulation time
    private FutureEventList futureEventList; // Future Event List
    private WorkloadGenerator workload; // Workload generator

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
        do {
            FutureEventList.Event event = this.futureEventList.nextEvent();
            if(event == FutureEventList.Event.ARRIVAL) {
                double nextArrivalTime = this.futureEventList.nextArrivalTime();
                double elapsedTime = nextArrivalTime - this.currentTime;
                this.currentTime = nextArrivalTime;
                arrivalHandler(elapsedTime, this.currentTime);
                // ricordare di passare anche il tempo del prossimo arrivo per eventuale continuo del codice
            } else {
                JobStats nextDepartureJob = this.futureEventList.nextDepartureJob();
                departureHandler(nextDepartureJob);
            }
        } while (this.currentTime < simulationDuration);

        //TODO: gestire nel caso trace driven se non ci sono più arrivi o altri eventi ma il tempo di simulazione non è ancora finito

        // Drain remaining jobs after simulation ends
        this.futureEventList.setNextArrivalTime(Double.POSITIVE_INFINITY); //sicuro da modificare
        while (!this.futureEventList.getJobStats().isEmpty()){
                JobStats nextDepartureJob = futureEventList.nextDepartureJob();
                departureHandler(nextDepartureJob);
        }
        /*
        logger.log(Level.INFO, "Simulation completed at time {0}\n", currentTime);
        logger.log(Level.INFO, "Total jobs processed: {0}\n", jobIdCounter);
        logger.log(Level.INFO, "Total jobs with negative remaining size: {0}\n", jobWithNegativeRemainingSize);
        logger.log(Level.INFO, "Negative percentage is {0,number,0.000}%\n", (100.0 * jobWithNegativeRemainingSize) / jobIdCounter);
        logger.log(Level.SEVERE, "Final seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));
         */

        csvAppenderJobs.close();
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
        /*
        this.csvAppenderServers.writeRow(
                this.currentTime.toString(),
                String.valueOf(this.loadBalancer.getJobCountsPerWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );
        */


        // Generate next arrival time
        workload.nextArrival(currentTime);
    }

    private void departureHandler(JobStats targetDepartureJobStats) {
        // Process elapsed time for all active jobs
        for (JobStats js : this.futureEventList.getJobStats()) {
            js.getJob().processForElapsedTime(targetDepartureJobStats.getEstimatedDepartureTime() - currentTime);
            //debugging
            if (js.getJob().hadNegativeRemainingSize) {
                jobWithNegativeRemainingSize++;
            }
        }

        // Update current time to departure time
        this.currentTime = targetDepartureJobStats.getEstimatedDepartureTime();

        // Process job departure through load balancer
        double responseTime = this.currentTime - targetDepartureJobStats.getArrivalTime();

        this.loadBalancer.departureJob(targetDepartureJobStats.getJob(), responseTime, this.currentTime);

        // Add to the csv for forensics analysis
        /*
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
                String.valueOf(this.loadBalancer.getJobCountsPerWebServer()),
                String.valueOf(this.loadBalancer.getWebServerCount()),
                String.valueOf(this.loadBalancer.getSpikeServerJobCount())
        );
        */

        this.jobStats.remove(targetDepartureJobStats);

        // Recalculate estimated departure times for remaining jobs
        for (JobStats js : this.jobStats) {
            js.estimateDepartureTime(this.currentTime);
        }
    }
}