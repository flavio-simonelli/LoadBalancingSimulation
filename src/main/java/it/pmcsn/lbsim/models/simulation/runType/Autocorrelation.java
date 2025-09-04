package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.AutoCorrelationFunction;
import it.pmcsn.lbsim.utils.OnlineACFOneStep;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;

public class Autocorrelation implements RunPolicy {
    private final CsvAppender autocorrCsv;
    private final OnlineACFOneStep acf;

    public Autocorrelation(int maxLag) {
        try {
            this.autocorrCsv = new CsvAppender(Path.of("output/csv/autocorrelation.csv"), "lag", "acf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.acf = new OnlineACFOneStep(maxLag);
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
        this.acf.add(responseTime);
    }

    @Override
    public void updateFinalStats() {
        // write the csv file
        double[] r = acf.getACF();
        for (int j = 0; j < r.length; j++) {
            autocorrCsv.writeRow(String.valueOf(j), String.valueOf(r[j]));
        }
    }

    @Override
    public void closeCsvs() {
        // close the csv file
        this.autocorrCsv.close();
    }
}
