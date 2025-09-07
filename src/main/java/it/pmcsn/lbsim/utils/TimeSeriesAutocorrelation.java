package it.pmcsn.lbsim.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TimeSeriesAutocorrelation {

    /**
     * Calcola la lag-k autocorrelazione di una lista di double.
     * Formula: sum((x[t]-mean)*(x[t-k]-mean)) / sum((x[t]-mean)^2)
     */
    public static double lagKAutocorrelation(List<Double> values, int k) {
        if (values.size() <= k) return Double.NaN;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double num = 0.0, den = 0.0;
        for (int t = k; t < values.size(); t++) {
            num += (values.get(t) - mean) * (values.get(t - k) - mean);
        }
        for (double v : values) {
            den += Math.pow(v - mean, 2);
        }

        return num / den;
    }

    /**
     * Legge dal CSV tutte le responseTime con time >= startTime.
     */
    public static List<Double> readResponseTimes(Path filePath, String separator, double startTime) throws IOException {
        List<Double> values = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String header = br.readLine(); // salta intestazione
            if (header == null) return values;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] tokens = line.split(separator);
                try {
                    double time = Double.parseDouble(tokens[0].trim());
                    double resp = Double.parseDouble(tokens[1].trim());
                    if (time >= startTime) {
                        values.add(resp);
                    }
                } catch (Exception e) {
                    // ignora righe malformate
                }
            }
        }
        return values;
    }

    public static void main(String[] args) throws IOException {
        Path csvFile = Path.of("output/csv/ResponseTuTTOReplica0.csv");
        double startTime = 120.0;
        int k = 50;

        List<Double> values = readResponseTimes(csvFile, ",", startTime);
        double rhoK = lagKAutocorrelation(values, k);

        System.out.println("****************************************");
        System.out.printf("AUTOCORRELATION FOR file=%s%n", csvFile.getFileName());
        System.out.printf("Start time >= %.3f, lag=%d%n", startTime, k);
        System.out.printf("r(%d) = %.4f%n", k, rhoK);
        System.out.println("****************************************");
    }
}
