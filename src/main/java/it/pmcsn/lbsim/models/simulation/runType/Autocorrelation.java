package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.AutoCorrelationFunction;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;

public class Autocorrelation implements RunPolicy {
    private final AutoCorrelationFunction acs;

    public Autocorrelation(int maxLag) {
        this.acs = new AutoCorrelationFunction(maxLag);
    }

    @Override
    public void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        // do nothing
    }

    @Override
    public void updateDepartureStats(int jobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        if (currentTime < 1200.0) {
            return; // skip the first 120 seconds to avoid transient effects
        }
        this.acs.add(responseTime); // response R0
    }

    @Override
    public void updateFinalStats() {
        acs.finish(); // esegue draining + normalizzazione
        double[] r = acs.getAcf();           // ACF[0..K], con r[0]=1.0
        double mean = acs.getMean();
        double stdev = acs.getStdDev();

        // opzionale: salva CSV
        acs.saveAcfToCsv(Path.of("output/csv/acf_run1.csv"));
    }

    @Override
    public void closeCsvs() {
    }
}
