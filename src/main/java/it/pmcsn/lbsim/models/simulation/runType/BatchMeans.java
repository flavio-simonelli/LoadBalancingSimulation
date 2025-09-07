package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.IntervalEstimation;
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
 * RunPolicy implementation with Batch Means method.
 * Colleziona statistiche per batch su:
 * - Response Time
 * - Utilization
 * - Mean Number of Jobs
 * - Requests distribution Spike vs Web
 * - Throughput for Spike and all spike servers
 */
public class BatchMeans implements RunPolicy {
    private final int batchSize;
    private int countTotalDeparture = 0;
    private int currentBatch = 0;
    private double time = 0;

    private final CsvAppender responseTimeCsv;
    private final CsvAppender utilizationCsv;
    private final CsvAppender meanJobsCsv;
    private final CsvAppender responseR0Csv;

    // Spike server metrics
    private final WelfordSimple responseTimeSpike = new WelfordSimple();
    private final TimeMediateWelford utilizationSpike = new TimeMediateWelford();
    private final TimeMediateWelford meanJobsSpike = new TimeMediateWelford();
    private int spikeRequestsProcessed = 0;

    // Dynamic maps for Web Servers (id -> tracker)
    private final Map<Integer, WelfordSimple> responseTimeWS = new HashMap<>();
    private final Map<Integer, TimeMediateWelford> utilizationWS = new HashMap<>();
    private final Map<Integer, TimeMediateWelford> meanJobsWS = new HashMap<>();
    private final Map<Integer, Integer> requestsWSProcessed = new HashMap<>();

    // scaling orizzontale
    private int scaleActions = 0;

    // Global R0
    private final WelfordSimple responseR0 = new WelfordSimple();

    private final IntervalEstimation intervalEstimation;
    private final static Logger logger = Logger.getLogger(BatchMeans.class.getName());

    public BatchMeans(int batchSize, float LOC) {
        this.intervalEstimation = new IntervalEstimation(LOC);
        this.batchSize = batchSize;
        try {
            responseTimeCsv = new CsvAppender(Path.of("output/csv/ResponseTimeSI160.csv"), "BatchID", "TotalDepartures", "ServerID", "Type", "NumDepartures", "Mean", "StdDev", "Variance", "SeminInterval", "%reqDirected", "Throughput");
            utilizationCsv = new CsvAppender(Path.of("output/csv/UtilizationSI160.csv"), "BatchID", "ServerID", "Type", "NumSamples", "Mean", "StdDev", "Variance");
            meanJobsCsv = new CsvAppender(Path.of("output/csv/MeanJobsSI160.csv"), "BatchID", "ServerID", "Type", "NumSamples", "Mean", "StdDev", "Variance");
            responseR0Csv = new CsvAppender(Path.of("output/csv/ResponseR0SI160.csv"), "BatchID", "TotalDepartures", "Mean", "StdDev", "Variance", "SeminIntervalR0", "horizontalscaleActions");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private int getRequestsProcessed(int id) {
        return requestsWSProcessed.computeIfAbsent(id, k -> 0);
    }

    private void incrementRequestsProcessed(int id) {
        requestsWSProcessed.put(id, getRequestsProcessed(id) + 1);
    }

    // ---------------- Simulation hooks ----------------

    @Override
    public void updateArrivalStats(double current, JobStats newJobStats, LoadBalancer loadBalancer) {
        // Spike
        utilizationSpike.iteration(loadBalancer.getSpikeServer().isBusy(), current);
        meanJobsSpike.iteration(loadBalancer.getSpikeServer().getCurrentSI(), current);
        // All Web Servers
        loadBalancer.getWebServers().getWebServers().forEach(ws -> {
            int id = ws.getId();
            getUtilizationTracker(id).iteration(ws.isBusy(), current);
            getMeanJobsTracker(id).iteration(ws.getCurrentSI(), current);
        });
    }

    @Override
    public void updateDepartureStats(double currentTime, JobStats departureJob, LoadBalancer loadBalancer, double responseTime) {
        countTotalDeparture++;
        // Spike update
        utilizationSpike.iteration(loadBalancer.getSpikeServer().isBusy(), currentTime);
        meanJobsSpike.iteration(loadBalancer.getSpikeServer().getCurrentSI(), currentTime);

        // Web Servers update
        double t = currentTime;
        loadBalancer.getWebServers().getWebServers().forEach(ws -> {
            int id = ws.getId();
            getUtilizationTracker(id).iteration(ws.isBusy(), t);
            getMeanJobsTracker(id).iteration(ws.getCurrentSI(), t);
        });
        // Response times + requests distribution
        if (departureJob.getJob().getAssignedServer().getId() == -1) {
            responseTimeSpike.iteration(responseTime);
            spikeRequestsProcessed++;
        } else {
            int id = departureJob.getJob().getAssignedServer().getId();
            getResponseTracker(id).iteration(responseTime);
            incrementRequestsProcessed(id);
        }
        responseR0.iteration(responseTime);

        // End of batch?
        if (countTotalDeparture == batchSize) {
            logger.log(Level.INFO, "Finished batch " + currentBatch);
            int newscaleAction = loadBalancer.getHorizontalScaler().getScaleActions();
            int scaledDiff = newscaleAction - scaleActions;
            scaleActions = newscaleAction;
            double elapsedTime = currentTime - time;
            time = currentTime;
            printCsvs(elapsedTime, scaledDiff);
            resetTrackers(time, loadBalancer);
            countTotalDeparture = 0;
            currentBatch++;
        }
    }

    @Override
    public void updateFinalStats() {
        // no-op for now
    }

    // ---------------- CSV printing ----------------

    private void printCsvs(double elapsedTime, int scaledDiff) {
        int totalDepartures = countTotalDeparture;

        // Spike
        writeResponseRow(
                currentBatch, totalDepartures,
                -1, "SPIKE", spikeRequestsProcessed,
                responseTimeSpike, elapsedTime
        );
        utilizationCsv.writeRow(
                String.valueOf(currentBatch),
                "-1", "SPIKE",
                String.valueOf(totalDepartures),
                String.valueOf(utilizationSpike.getMean()),
                String.valueOf(utilizationSpike.getStdDev()),
                String.valueOf(utilizationSpike.getVariance())
        );
        meanJobsCsv.writeRow(
                String.valueOf(currentBatch),
                "-1", "SPIKE",
                String.valueOf(totalDepartures),
                String.valueOf(meanJobsSpike.getMean()),
                String.valueOf(meanJobsSpike.getStdDev()),
                String.valueOf(meanJobsSpike.getVariance())
        );

        // Web Servers
        for (int id : responseTimeWS.keySet()) {
            WelfordSimple resp = responseTimeWS.get(id);
            TimeMediateWelford util = utilizationWS.get(id);
            TimeMediateWelford jobs = meanJobsWS.get(id);
            int numProcessed = getRequestsProcessed(id);

            writeResponseRow(
                    currentBatch, totalDepartures,
                    id, "WEB", numProcessed,
                    resp, elapsedTime
            );
            utilizationCsv.writeRow(
                    String.valueOf(currentBatch),
                    String.valueOf(id), "WEB",
                    String.valueOf(totalDepartures),
                    String.valueOf(util.getMean()),
                    String.valueOf(util.getStdDev()),
                    String.valueOf(util.getVariance())
            );
            meanJobsCsv.writeRow(
                    String.valueOf(currentBatch),
                    String.valueOf(id), "WEB",
                    String.valueOf(totalDepartures),
                    String.valueOf(jobs.getMean()),
                    String.valueOf(jobs.getStdDev()),
                    String.valueOf(jobs.getVariance())
            );
        }
        // R0 global
        responseR0Csv.writeRow(
                String.valueOf(currentBatch),
                String.valueOf(totalDepartures),
                String.valueOf(responseR0.getAvg()),
                String.valueOf(responseR0.getStandardVariation()),
                String.valueOf(responseR0.getVariance()),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        responseR0.getStandardVariation(), responseR0.getI())),
                String.valueOf(scaledDiff)
        );
    }

    private void writeResponseRow(int batchId, int totalDepartures,
                                  int serverId, String type, int numProcessed,
                                  WelfordSimple respStats, double elapsedTime) {
        double percDirected = totalDepartures > 0 ? (100.0 * numProcessed / totalDepartures) : 0.0;
        double throughput = elapsedTime > 0 ? (numProcessed / elapsedTime) : 0.0;

        responseTimeCsv.writeRow(
                String.valueOf(batchId),
                String.valueOf(totalDepartures),
                String.valueOf(serverId),
                type,
                String.valueOf(numProcessed),
                String.valueOf(respStats.getAvg()),
                String.valueOf(respStats.getStandardVariation()),
                String.valueOf(respStats.getVariance()),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        respStats.getStandardVariation(), respStats.getI())),
                String.format("%.2f", percDirected),
                String.format("%.6f", throughput)
        );
    }

    public void closeCsvs() {
        this.responseTimeCsv.close();
        this.meanJobsCsv.close();
        this.utilizationCsv.close();
        this.responseR0Csv.close();
    }

    // ---------------- Helpers ----------------

    private void resetTrackers(double currentTime, LoadBalancer lb) {
        responseTimeSpike.reset();
        utilizationSpike.reset(currentTime, lb.getSpikeServer().isBusy());
        meanJobsSpike.reset(currentTime, lb.getSpikeServer().getCurrentSI());

        responseR0.reset();
        responseTimeWS.values().forEach(WelfordSimple::reset);

        // reset per ogni web server allo stato corrente
        lb.getWebServers().getWebServers().forEach(ws -> {
            int id = ws.getId();
            utilizationWS.get(id).reset(currentTime, ws.isBusy());
            meanJobsWS.get(id).reset(currentTime, ws.getCurrentSI());
        });

        requestsWSProcessed.clear();
        spikeRequestsProcessed = 0;
    }

}
