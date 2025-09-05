package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.AutoCorrelationFunction;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;

public class Autocorrelation implements RunPolicy {
    private final AutoCorrelationFunction acf;

    public Autocorrelation(int maxLag) {
        this.acf = new AutoCorrelationFunction(maxLag, "autocorrelation.csv");
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
        this.acf.add(responseTime); // response R0
    }

    @Override
    public void updateFinalStats() {
        // write the csv file
        this.acf.saveToCsv();
        System.out.println("CUTOFF VALUE "+this.acf.findCutoffLag(0.05,10));
    }

    @Override
    public void closeCsvs() {
    }
}
