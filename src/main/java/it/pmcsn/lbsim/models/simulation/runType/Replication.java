package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.TimeMediateWelford;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RunPolicy implementation with Replication method.
 * Ogni replica produce:
 *   - un file "ReplicaX.csv" con righe per ogni evento (arrivo/departure) e metriche per ogni server.
 *   - un file "ResponseR0ReplicaX.csv" con righe per ogni evento e metriche globali R0.
 */
public class Replication implements RunPolicy {
    private int replica = 0;
    private final int numberOfReplicas;
    // CSV writers per replica
    private CsvAppender perServerCsv;
    private CsvAppender r0Csv;
    private CsvAppender allDataCsv;
    // Spike trackers
    private final WelfordSimple responseTimeSpike = new WelfordSimple();
    private final TimeMediateWelford utilizationSpike = new TimeMediateWelford();
    private final TimeMediateWelford meanJobsSpike = new TimeMediateWelford();
    // Web server trackers
    private final Map<Integer, WelfordSimple> responseTimeWS = new HashMap<>();
    private final Map<Integer, TimeMediateWelford> utilizationWS = new HashMap<>();
    private final Map<Integer, TimeMediateWelford> meanJobsWS = new HashMap<>();
    // Global R0
    private final WelfordSimple responseR0 = new WelfordSimple();

    private final static Logger logger = Logger.getLogger(Replication.class.getName());

    public Replication(int numberOfReplicas) {
        if (numberOfReplicas <= 0) {
            throw new IllegalArgumentException("Number of replicas must be > 0");
        }
        this.numberOfReplicas = numberOfReplicas;
        openCsvsForReplica();
    }

    // ---------------- Helpers ----------------

    private WelfordSimple getResponseTracker(int id) {
        return responseTimeWS.computeIfAbsent(id, k -> new WelfordSimple());
    }

    private TimeMediateWelford getUtilizationTracker(int id) {
        return utilizationWS.computeIfAbsent(id, k -> new TimeMediateWelford());
    }

    private TimeMediateWelford getMeanJobsTracker(int id) {
        return meanJobsWS.computeIfAbsent(id, k -> new TimeMediateWelford());
    }

    private void openCsvsForReplica() {
        try {
            perServerCsv = new CsvAppender(
                    Path.of("output/csv/Replica" + replica + ".csv"), "Time", "ServerID", "Type", "MeanResponseTime", "StdDevResponseTime", "VarianceResponseTime", "MeanUtilization", "StdDevUtilization", "VarianceUtilization", "MeanJobs", "StdDevMeanJobs", "VarianceMeanJobs"
            );
            r0Csv = new CsvAppender(
                    Path.of("output/csv/ResponseR0Replica" + replica + ".csv"), "Time", "MeanResponseTime", "StdDevResponseTime", "VarianceResponseTime");
            allDataCsv = new CsvAppender(
                    Path.of("output/csv/ResponseTuTTOReplica" + replica + ".csv"), "Time", "ServerID", "ResponseTime", "SI"); // da togliere
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeCsvsForReplica() {
        if (perServerCsv != null) perServerCsv.close();
        if (r0Csv != null) r0Csv.close();
        if (allDataCsv != null) allDataCsv.close(); // da togliere
    }

    // ---------------- Simulation hooks ----------------

    @Override
    public void updateArrivalStats(double current, JobStats newJobStats, LoadBalancer loadBalancer) {
        // Spike update
        utilizationSpike.iteration(loadBalancer.getSpikeServer().isBusy(), current);
        meanJobsSpike.iteration(loadBalancer.getSpikeServer().getCurrentSI(), current);

        // Web servers update
        loadBalancer.getWebServers().getWebServers().forEach(ws -> {
            int id = ws.getId();
            getUtilizationTracker(id).iteration(ws.isBusy(), current);
            getMeanJobsTracker(id).iteration(ws.getCurrentSI(), current);
        });

        // Scrittura riga su CSV (senza response time, solo utilizzo e jobs)
        writePerServerRows(current);
        writeR0Row(current);
    }

    @Override
    public void updateDepartureStats(double currentTime, JobStats departureJob,
                                     LoadBalancer loadBalancer, double responseTime) {
        // Spike updates
        utilizationSpike.iteration(loadBalancer.getSpikeServer().isBusy(), currentTime);
        meanJobsSpike.iteration(loadBalancer.getSpikeServer().getCurrentSI(), currentTime);

        // Web Servers updates
        loadBalancer.getWebServers().getWebServers().forEach(ws -> {
            int id = ws.getId();
            getUtilizationTracker(id).iteration(ws.isBusy(), currentTime);
            getMeanJobsTracker(id).iteration(ws.getCurrentSI(), currentTime);
        });

        // Response time update
        if (departureJob.getJob().getAssignedServer().getId() == -1) {
            responseTimeSpike.iteration(responseTime);
        } else {
            int id = departureJob.getJob().getAssignedServer().getId();
            getResponseTracker(id).iteration(responseTime);
        }

        // R0 update
        responseR0.iteration(responseTime);

        //TODO: da togliere
        try {
            allDataCsv.writeRow(
                    String.valueOf(currentTime),
                    String.valueOf(departureJob.getJob().getAssignedServer().getId()),
                    String.valueOf(responseTime),
                    String.valueOf(departureJob.getJob().getAssignedServer().getCurrentSI())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Scrittura riga su CSV
        writePerServerRows(currentTime);
        writeR0Row(currentTime);
    }

    @Override
    public void updateFinalStats() {
        logger.log(Level.INFO, "Closing replica " + replica);

        closeCsvsForReplica();

        // Reset trackers per nuova replica
        resetTrackers();

        // Nuova replica
        replica++;
        if (replica > numberOfReplicas) {
            logger.log(Level.INFO, "All replicas completed.");
            throw new RuntimeException("All replicas completed.");
        } else {
            openCsvsForReplica();
        }
    }

    @Override
    public void closeCsvs() {
        closeCsvsForReplica();
    }

    // ---------------- CSV writing ----------------

    private void writePerServerRows(double time) {
        // Spike row
        perServerCsv.writeRow(
                String.valueOf(time),
                "-1", "SPIKE",
                String.valueOf(responseTimeSpike.getAvg()),
                String.valueOf(responseTimeSpike.getStandardVariation()),
                String.valueOf(responseTimeSpike.getVariance()),
                String.valueOf(utilizationSpike.getMean()),
                String.valueOf(utilizationSpike.getStdDev()),
                String.valueOf(utilizationSpike.getVariance()),
                String.valueOf(meanJobsSpike.getMean()),
                String.valueOf(meanJobsSpike.getStdDev()),
                String.valueOf(meanJobsSpike.getVariance())
        );

        // Web server rows
        for (int id : responseTimeWS.keySet()) {
            WelfordSimple resp = responseTimeWS.get(id);
            TimeMediateWelford util = utilizationWS.get(id);
            TimeMediateWelford jobs = meanJobsWS.get(id);

            perServerCsv.writeRow(
                    String.valueOf(time),
                    String.valueOf(id), "WEB",
                    String.valueOf(resp.getAvg()),
                    String.valueOf(resp.getStandardVariation()),
                    String.valueOf(resp.getVariance()),
                    String.valueOf(util.getMean()),
                    String.valueOf(util.getStdDev()),
                    String.valueOf(util.getVariance()),
                    String.valueOf(jobs.getMean()),
                    String.valueOf(jobs.getStdDev()),
                    String.valueOf(jobs.getVariance())
            );
        }
    }

    private void writeR0Row(double time) {
        r0Csv.writeRow(
                String.valueOf(time),
                String.valueOf(responseR0.getAvg()),
                String.valueOf(responseR0.getStandardVariation()),
                String.valueOf(responseR0.getVariance())
        );
    }

    // ---------------- Helpers ----------------

    private void resetTrackers() {
        responseTimeSpike.reset();
        utilizationSpike.reset();
        meanJobsSpike.reset();

        responseR0.reset();
        responseTimeWS.values().forEach(WelfordSimple::reset);
        utilizationWS.values().forEach(TimeMediateWelford::reset);
        meanJobsWS.values().forEach(TimeMediateWelford::reset);
    }
}
