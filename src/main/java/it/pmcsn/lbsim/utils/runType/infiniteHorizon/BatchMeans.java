package it.pmcsn.lbsim.utils.runType.infiniteHorizon;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.TimeMediateWelford;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.runType.RunPolicy;

import java.io.IOException;
import java.nio.file.Path;

public class BatchMeans implements RunPolicy {

    private final int batchSize;
    private final int k;

    private int currentBatch = 0;

    private final CsvAppender jobLogCsv ;
    private final  CsvAppender serverLogCsv;
    private final CsvAppender departureStatsCsv;
    private final CsvAppender welfordCsv;

    private final WelfordSimple arrivalStats = new WelfordSimple();
    private WelfordSimple departureStats = new WelfordSimple();
    private final TimeMediateWelford meanNumberJobs = new TimeMediateWelford();
    private final IntervalEstimation intervalEstimation = new IntervalEstimation(0.95);


    public BatchMeans(int k, int batchSize, int seed) {
        this.batchSize = batchSize;
        this.k = k;
        try {
            welfordCsv = new CsvAppender(
                    Path.of("output/csv/Welford" + seed + "rep.csv"),
                    "BatchID", "N", "Mean", "StdDev", "Variance", "SemiInterval"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            jobLogCsv = new CsvAppender(Path.of("output/csv/Jobstats"+seed+".csv"), "JobID", "Arrival", "Departure", "ResponseTime", "OriginalSize", "AssignedServerID");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            serverLogCsv = new CsvAppender(Path.of("output/csv/Serverstats"+seed+".csv"), "Time", "ServerID", "State", "ActualSI");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            departureStatsCsv = new CsvAppender(Path.of("output/csv/Departures"+seed+".csv"), "Time", "NumJobs", "ProxArrival", "ProxDepartureofAllJobs", "RemainingSizeOfAllJobs", "NumServers");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList){
        saveCsvForArrival(loadBalancer, futureEventList, currentTime);
        arrivalStats.iteration(size);
        meanNumberJobs.iteration(currentJobCount, currentTime);
    }
    @Override
    public void updateDepartureStats(int numJobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList){
        saveCsvForDeparture(jobStats,futureEventList,currentTime, responseTime, loadBalancer);
        this.departureStats.iteration(responseTime);
        this.meanNumberJobs.iteration(numJobs,currentTime);
        if ( departureStats >= batchSize) {
            saveData();
            this.departureStats = new WelfordSimple();
            currentBatch++;
        }
    }

    @Override
    public void updateFinalStats() {
        this.welfordCsv.close();
        this.departureStatsCsv.close();
        this.serverLogCsv.close();
        this.jobLogCsv.close();

    }

    private void saveData() {
        int n = departureStats.getI();
        if (n == 0) return; // niente da salvare

        double mean = departureStats.getAvg();
        double std = departureStats.getStandardVariation();
        double var = departureStats.getVariance();
        double semi = intervalEstimation.semiIntervalEstimation(std, n);

        try {
            welfordCsv.writeRow(
                    String.valueOf(currentBatch),
                    String.valueOf(n),
                    String.valueOf(mean),
                    String.valueOf(std),
                    String.valueOf(var),
                    String.valueOf(semi)
            );
        } catch (Exception e) {
            throw new RuntimeException("Errore scrivendo batch " + currentBatch, e);
        }
    }


    private void saveCsvForArrival(LoadBalancer loadBalancer, FutureEventList futureEventList, Double currentTime){
        try{
            for (var server : loadBalancer.getWebServers().getWebServers()) {
                serverLogCsv.writeRow(
                        String.valueOf(currentTime),
                        String.valueOf(server.getId()),
                        String.valueOf(loadBalancer.getWebServers().isRemovingServer(server)),
                        String.valueOf(server.getCurrentSI())
                );
            }
            serverLogCsv.writeRow(
                    String.valueOf(currentTime),
                    String.valueOf(loadBalancer.getSpikeServer().getId()),
                    "false",
                    String.valueOf(loadBalancer.getSpikeServer().getCurrentSI())
            );
            departureStatsCsv.writeRow(
                    String.valueOf(currentTime),
                    String.valueOf(loadBalancer.getCurrentJobCount()),
                    String.valueOf(futureEventList.getnextArrivalTime()),
                    String.valueOf(futureEventList.getAllDepartureTimes()),
                    String.valueOf(loadBalancer.getWebServers().getRemianingSizeForAllJobs()),
                    String.valueOf(loadBalancer.getWebServers().getWebServerCount())
            );
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    public void saveCsvForDeparture(JobStats jobStats, FutureEventList futureEventList, double currentTime, double responseTime, LoadBalancer loadBalancer) {
        try {
            jobLogCsv.writeRow(
                    String.valueOf(jobStats.getJob().getJobId()),
                    String.format("%.6f", jobStats.getArrivalTime()),
                    String.format("%.6f", currentTime),
                    String.format("%.6f", responseTime),
                    String.format("%.6f", jobStats.getOriginalSize()),
                    String.valueOf(jobStats.getJob().getAssignedServer().getId())
            );
            for (var server : loadBalancer.getWebServers().getWebServers()) {
                serverLogCsv.writeRow(
                        String.valueOf(currentTime),
                        String.valueOf(server.getId()),
                        String.valueOf(loadBalancer.getWebServers().isRemovingServer(server)),
                        String.valueOf(server.getCurrentSI())
                );
            }
            serverLogCsv.writeRow(
                    String.valueOf(currentTime),
                    String.valueOf(loadBalancer.getSpikeServer().getId()),
                    "false",
                    String.valueOf(loadBalancer.getSpikeServer().getCurrentSI())
            );
            departureStatsCsv.writeRow(
                    String.valueOf(currentTime),
                    String.valueOf(loadBalancer.getCurrentJobCount()),
                    String.valueOf(futureEventList.getnextArrivalTime()),
                    String.valueOf(futureEventList.getAllDepartureTimes()),
                    String.valueOf(loadBalancer.getWebServers().getRemianingSizeForAllJobs()),
                    String.valueOf(loadBalancer.getWebServers().getWebServerCount())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
