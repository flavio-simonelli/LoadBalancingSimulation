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
    private final CsvAppender autocorrCsv;

    public Autocorrelation(int maxLag) {
        this.acs = new AutoCorrelationFunction(maxLag);
        try {
            this.autocorrCsv = new CsvAppender(Path.of("output/csv/autocorrelation.csv"), "lag", "autocorrelation");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        // do nothing
    }

    @Override
    public void updateDepartureStats(int jobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        this.acs.iteration(responseTime); // response R0
    }

    @Override
    public void updateFinalStats() {
        acs.finish();
        double[] r = acs.autocorrelation();           // ACF[0..K], con r[0]=1.0
        for (int j = 0; j < r.length; j++) {
            autocorrCsv.writeRow(String.valueOf(j), String.valueOf(r[j]));
        }
    }

    @Override
    public void closeCsvs() {
        autocorrCsv.close();
    }
}
