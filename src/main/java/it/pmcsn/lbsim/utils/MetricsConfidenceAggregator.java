package it.pmcsn.lbsim.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class MetricsConfidenceAggregator {

    // Parametri hardcoded
    private static final double START_TIME = 172800.0; //
    private static final double END_TIME = 208800.0;   //
    private static final double STEP = 300.0;
    private static final double LOC = 0.95;

    private static final String INPUT_DIR = "output/csv/";
    private static final String[] METRICS = {
            "ResponseMeanReplica",
            "ServersActiveReplica",
            "SpikeUtilReplica"
    };

    public static void main(String[] args) {
        IntervalEstimation estimator = new IntervalEstimation(LOC);

        for (String metric : METRICS) {
            try {
                // Trova tutti i file della metrica
                List<Path> files = Files.list(Paths.get(INPUT_DIR))
                        .filter(f -> f.getFileName().toString().startsWith(metric))
                        .sorted()
                        .collect(Collectors.toList());

                if (files.isEmpty()) {
                    System.err.println("⚠️ Nessun file trovato per " + metric);
                    continue;
                }

                // File di output
                String outFile = INPUT_DIR + metric + "_IC.csv";
                try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
                    writer.println("Time,Mean,CI");

                    for (double t = START_TIME; t <= END_TIME; t += STEP) {
                        List<Double> valuesAtT = new ArrayList<>();

                        // Per ogni replica, cerca il valore al tempo t (o ultimo <= t)
                        for (Path file : files) {
                            Double value = getValueAtOrBeforeTime(file, t);
                            if (value != null) valuesAtT.add(value);
                        }

                        if (valuesAtT.size() < 2) continue; // serve almeno 2 repliche

                        // Calcola media
                        double mean = valuesAtT.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                        // Calcola deviazione standard
                        double sum = 0.0;
                        for (double v : valuesAtT) sum += Math.pow(v - mean, 2);
                        double std = Math.sqrt(sum / (valuesAtT.size() - 1));

                        // Semi-intervallo
                        double semi = estimator.semiIntervalEstimation(std, valuesAtT.size());

                        writer.printf(Locale.US, "%.2f,%.6f,%.6f%n", t, mean, semi);
                    }
                }

                System.out.println("✅ Scritto: " + outFile);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Legge un file CSV e restituisce il valore della seconda colonna
     * al tempo esatto t o, se non esiste, l’ultimo valore <= t
     */
    private static Double getValueAtOrBeforeTime(Path file, double t) {
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String line = br.readLine(); // header
            Double lastVal = null;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                double time = Double.parseDouble(parts[0]);
                double val = Double.parseDouble(parts[1]);

                if (time > t) break;
                lastVal = val;
            }
            return lastVal;
        } catch (Exception e) {
            System.err.println("Errore in " + file + " → " + e.getMessage());
            return null;
        }
    }
}
