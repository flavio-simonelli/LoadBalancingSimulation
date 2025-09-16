package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.TimeMediateWelford;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProvaRunPolicy implements RunPolicy {
    private int replica = 0;
    private final int numberOfReplicas;
    private final WelfordSimple responseR0 = new WelfordSimple();
    private final TimeMediateWelford spikeUtil = new TimeMediateWelford();
    private static final Logger logger = Logger.getLogger(ProvaRunPolicy.class.getName());


    private CsvAppender responseCsv;
    private CsvAppender serversCsv;
    private CsvAppender spikeCsv;

    public ProvaRunPolicy(int numberOfReplicas) {
        if (numberOfReplicas <= 0) {
            throw new IllegalArgumentException("Number of replicas must be > 0");
        }
        this.numberOfReplicas = numberOfReplicas;
        openCsvsForReplica();
    }

    @Override
    public void updateArrivalStats(double current, JobStats newJobStats, LoadBalancer lb) {
        // niente da fare sugli arrivi
    }

    @Override
    public void updateDepartureStats(double currentTime, JobStats departureJob,
                                     LoadBalancer lb, double responseTime) {
        // === Tempo di risposta medio ===
        responseR0.iteration(responseTime);
        int active = lb.getWebServers().getWebServers().size() +
                lb.getWebServers().getRemovingServers().size();
        spikeUtil.iteration(lb.getSpikeServer().isBusy(), currentTime);
        if (currentTime >= 172800.0) {
            responseCsv.writeRow(String.valueOf(currentTime),
                    String.valueOf(responseR0.getAvg()));
            serversCsv.writeRow(String.valueOf(currentTime),
                    String.valueOf(active));
            spikeCsv.writeRow(String.valueOf(currentTime),
                    String.valueOf(spikeUtil.getMean()));
        }
    }

    @Override
    public void updateFinalStats() {
        logger.log(Level.INFO, "Closing replica " + replica);
        closeCsvs();
        resetTrackers();

        replica++;
        if (replica > numberOfReplicas) {
            logger.log(Level.INFO, "All replicas completed.");
            throw new RuntimeException("All replicas completed.");
        } else {
            openCsvsForReplica();
        }
    }

    private void openCsvsForReplica() {
        try {
            responseCsv = new CsvAppender(
                    Path.of("output/csv/ResponseMeanReplica" + replica + ".csv"),
                    "Time", "MeanResponseTime"
            );
            serversCsv = new CsvAppender(
                    Path.of("output/csv/ServersActiveReplica" + replica + ".csv"),
                    "Time", "ActiveServers"
            );
            spikeCsv = new CsvAppender(
                    Path.of("output/csv/SpikeUtilReplica" + replica + ".csv"),
                    "Time", "SpikeUtilization"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeCsvs() {
        if (responseCsv != null) responseCsv.close();
        if (serversCsv != null) serversCsv.close();
        if (spikeCsv != null) spikeCsv.close();
    }

    private void resetTrackers() {
        responseR0.reset();
        spikeUtil.reset();
    }
}
