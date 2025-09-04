package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Replication implements  RunPolicy {
    private int replica = 0;
    private final WelfordSimple responseTimeWebServerWelford;
    private final CsvAppender replicationResponseTime;
    private List<Double> replicaMeans = new ArrayList<>();
    private final IntervalEstimation intervalEstimation;

    public Replication(float LOC) {
        this.intervalEstimation = new IntervalEstimation(LOC);
        this.responseTimeWebServerWelford = new WelfordSimple();
        try {
            this.replicationResponseTime = new CsvAppender(Path.of("output/csv/responseTimeReplication.csv"), "Replica", "MeanResponseTime", "StdDev", "Variance");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void updateArrivalStats(double size, int currentJobCount, Double currentTime, LoadBalancer loadBalancer, FutureEventList futureEventList) {

    }

    @Override
    public void updateDepartureStats(int jobs, double currentTime, double responseTime, JobStats jobStats, LoadBalancer loadBalancer, FutureEventList futureEventList) {
        responseTimeWebServerWelford.iteration(responseTime);
    }

    @Override
    public void updateFinalStats() {
        // write the csv file
        this.replicationResponseTime.writeRow(String.valueOf(this.replica),
                String.valueOf(this.responseTimeWebServerWelford.getAvg()),
                String.valueOf(this.responseTimeWebServerWelford.getStandardVariation()),
                String.valueOf(this.responseTimeWebServerWelford.getVariance()));
        // add the mean of this replica to the list
        this.replicaMeans.add(this.responseTimeWebServerWelford.getAvg());
        this.responseTimeWebServerWelford.reset();
    }

    @Override
    public void closeCsvs() {
        this.replicationResponseTime.close();
    }

    public void nextReplica() {
        this.replica++;
    }

    // calcola la media delle medie per ogni replica e la loro standard variation e crea l'intervallo di confidenza
    public void finalCalculation() {
        double ie = intervalEstimation.semiIntervalEstimation(getSampleStdDev(), getSampleSize());
        System.out.println("Media repliche: " + getMean());
        System.out.println("StdDev campionaria: " + getSampleStdDev());
        System.out.println("Numero repliche: " + getSampleSize());
        System.out.println("Semintervallo: " + ie);
        System.out.println("Intervallo di confidenza: " + (getMean() - ie) + " - " + (getMean() + ie));
        checkApproxNormality();
    }

    // Media delle repliche
    private double getMean() {
        return replicaMeans.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }

    // Deviazione standard campionaria delle repliche
    private double getSampleStdDev() {
        int n = replicaMeans.size();
        if (n < 2) {
            return Double.NaN; // Non definita per meno di 2 osservazioni
        }
        double mean = getMean();
        double sumSq = replicaMeans.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .sum();
        return Math.sqrt(sumSq / (n - 1));
    }

    // Numero di repliche
    private int getSampleSize() {
        return replicaMeans.size();
    }

    // Test di normalità semplificato (skewness + kurtosis)
    private void checkApproxNormality() {
        int n = getSampleSize();
        double mean = getMean();
        double stdDev = getSampleStdDev();

        if (n < 3 || Double.isNaN(stdDev) || stdDev == 0) {
            System.out.println("Campione troppo piccolo per verificare normalità.");
            return;
        }

        // Calcolo skewness
        double skewness = replicaMeans.stream()
                .mapToDouble(x -> Math.pow((x - mean) / stdDev, 3))
                .sum() / n;

        // Calcolo kurtosis (excess kurtosis)
        double kurtosis = replicaMeans.stream()
                .mapToDouble(x -> Math.pow((x - mean) / stdDev, 4))
                .sum() / n - 3.0;

        System.out.printf("Skewness: %.4f, Kurtosis: %.4f%n", skewness, kurtosis);
        System.out.println("Se skewness ≈ 0 e kurtosis ≈ 0 ⇒ distribuzione vicina a Normale.");
    }


}
