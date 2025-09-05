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
import java.util.logging.Logger;
import java.util.logging.Level;

public class BatchMeans implements RunPolicy {
    private final int batchSize;
    private int countDeparture = 0;
    private int currentBatch = 0;
    private final CsvAppender responceTimeCsv;
    private final CsvAppender utilizationCsv;
    private final CsvAppender meanNumberJobsCsv;
    private final WelfordSimple responceTimeWebServerWelford = new WelfordSimple();
    private final WelfordSimple responceTimeSpikeServerWelford = new WelfordSimple();
    private final TimeMediateWelford utilizationWebServerWelford = new TimeMediateWelford();
    private final TimeMediateWelford utilizationSpikeServerWelford = new TimeMediateWelford();
    private final TimeMediateWelford meanNumberJobsWebServerWelford = new TimeMediateWelford();
    private final TimeMediateWelford meanNumberJobsSpikeServerWelford = new TimeMediateWelford();
    private final IntervalEstimation intervalEstimation;
    private final static Logger logger = Logger.getLogger(BatchMeans.class.getName());


    public BatchMeans(int batchSize, float LOC) {
        this.intervalEstimation = new IntervalEstimation(LOC);
        this.batchSize = batchSize;
        try {
            responceTimeCsv = new CsvAppender(Path.of("output/csv/ResponceTime.csv"), "BatchID", "NumberOfDeparture", "MeanWS", "MeanSS", "StdDevWS", "StdDevSS", "VarianceWS", "VarianceSS", "SemiIntervalWB", "SemiIntervalSS");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            utilizationCsv = new CsvAppender(Path.of("output/csv/Utilization.csv"), "BatchID", "NumberOfDeparture", "MeanWS", "MeanSS", "StdDevWS", "StdDevSS", "VarianceWS", "VarianceSS", "SemiIntervalWS", "SemiIntervalSS");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            meanNumberJobsCsv = new CsvAppender(Path.of("output/csv/MeanNumberJobs.csv"), "BatchID", "NumberOfDeparture", "MeanWS", "MeanSS", "StdDevWS", "StdDevSS", "VarianceWS", "VarianceSS", "SemiIntervalWS", "SemiIntervalSS");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        this.utilizationSpikeServerWelford.iteration(loadBalancer.getSpikeServer().getCurrentSI() > 0 ? 1 : 0, currentTime);
        this.utilizationWebServerWelford.iteration(loadBalancer.getWebServers().getJobCount(0) > 0 ? 1 : 0, currentTime);
        this.meanNumberJobsSpikeServerWelford.iteration(loadBalancer.getSpikeServer().getCurrentSI(), currentTime);
        this.meanNumberJobsWebServerWelford.iteration(loadBalancer.getWebServers().getJobCount(0), currentTime);
    }

    @Override
    public void updateDepartureStats(int numJobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        countDeparture++;
        this.utilizationSpikeServerWelford.iteration(loadBalancer.getSpikeServer().getCurrentSI() > 0 ? 1 : 0, currentTime);
        this.utilizationWebServerWelford.iteration(loadBalancer.getWebServers().getJobCount(0) > 0 ? 1 : 0, currentTime);
        this.meanNumberJobsSpikeServerWelford.iteration(loadBalancer.getSpikeServer().getCurrentSI(), currentTime);
        this.meanNumberJobsWebServerWelford.iteration(loadBalancer.getWebServers().getJobCount(0), currentTime);
        if (jobStats.getJob().getAssignedServer().getId() == -1) {
            this.responceTimeSpikeServerWelford.iteration(responseTime);
        } else {
            this.responceTimeWebServerWelford.iteration(responseTime);
        }
        if (countDeparture == batchSize) {
            logger.log(Level.INFO, "Finite Batch " + currentBatch);
            printCsvs();
            countDeparture = 0;
            currentBatch++;
            this.responceTimeWebServerWelford.reset();
            this.responceTimeSpikeServerWelford.reset();
            this.utilizationWebServerWelford.reset();
            this.utilizationSpikeServerWelford.reset();
            this.meanNumberJobsWebServerWelford.reset();
            this.meanNumberJobsSpikeServerWelford.reset();
        }
    }

    @Override
    public void updateFinalStats() {
    }

    private void printCsvs() {
        responceTimeCsv.writeRow(
                String.valueOf(currentBatch),
                String.valueOf(countDeparture),
                String.valueOf(responceTimeWebServerWelford.getAvg()),
                String.valueOf(responceTimeSpikeServerWelford.getAvg()),
                String.valueOf(responceTimeWebServerWelford.getStandardVariation()),
                String.valueOf(responceTimeSpikeServerWelford.getStandardVariation()),
                String.valueOf(responceTimeWebServerWelford.getVariance()),
                String.valueOf(responceTimeSpikeServerWelford.getVariance()),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        responceTimeWebServerWelford.getStandardVariation(),
                        responceTimeWebServerWelford.getI()
                )),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        responceTimeSpikeServerWelford.getStandardVariation(),
                        responceTimeSpikeServerWelford.getI()
                ))
        );
        meanNumberJobsCsv.writeRow(
                String.valueOf(currentBatch),
                String.valueOf(countDeparture),
                String.valueOf(meanNumberJobsWebServerWelford.getAvg()),
                String.valueOf(meanNumberJobsSpikeServerWelford.getAvg()),
                String.valueOf(meanNumberJobsWebServerWelford.getStandardVariation()),
                String.valueOf(meanNumberJobsSpikeServerWelford.getStandardVariation()),
                String.valueOf(meanNumberJobsWebServerWelford.getVariance()),
                String.valueOf(meanNumberJobsSpikeServerWelford.getVariance()),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        meanNumberJobsWebServerWelford.getStandardVariation(),
                        meanNumberJobsWebServerWelford.getI()
                )),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        meanNumberJobsSpikeServerWelford.getStandardVariation(),
                        meanNumberJobsSpikeServerWelford.getI()
                ))
        );
        utilizationCsv.writeRow(
                String.valueOf(currentBatch),
                String.valueOf(countDeparture),
                String.valueOf(utilizationWebServerWelford.getAvg()),
                String.valueOf(utilizationSpikeServerWelford.getAvg()),
                String.valueOf(utilizationWebServerWelford.getStandardVariation()),
                String.valueOf(utilizationSpikeServerWelford.getStandardVariation()),
                String.valueOf(utilizationWebServerWelford.getVariance()),
                String.valueOf(utilizationSpikeServerWelford.getVariance()),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        utilizationWebServerWelford.getStandardVariation(),
                        utilizationWebServerWelford.getI()
                )),
                String.valueOf(intervalEstimation.semiIntervalEstimation(
                        utilizationSpikeServerWelford.getStandardVariation(),
                        utilizationSpikeServerWelford.getI()
                ))
        );
    }

    public void closeCsvs() {
        this.responceTimeCsv.close();
        this.meanNumberJobsCsv.close();
        this.utilizationCsv.close();
    }
}
