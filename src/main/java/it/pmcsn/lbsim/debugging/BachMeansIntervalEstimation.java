package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class BachMeansIntervalEstimation {

    // Conta il numero di righe (escludendo l'header)
    public static int countRows(String csv) throws Exception {
        return Math.toIntExact(Math.max(0, java.nio.file.Files.lines(Paths.get(csv)).count() - 1));
    }

    // Calcola la media dei valori R0captured nel CSV
    public static double grandMean(String csv, String targetColumn) throws Exception {
        var lines = java.nio.file.Files.readAllLines(Paths.get(csv));
        int idx = Arrays.asList(lines.get(0).split(",")).indexOf(targetColumn);
        if (idx < 0) throw new IllegalArgumentException("Colonna "+targetColumn+" non trovata");
        double[] vals = lines.stream().skip(1).map(l -> l.split(","))
                .filter(a -> a.length > idx && !a[idx].isBlank())
                .mapToDouble(a -> {
                    try { return Double.parseDouble(a[idx]); } catch (Exception e) { return Double.NaN; }
                })
                .filter(d -> !Double.isNaN(d))
                .toArray();
        return Arrays.stream(vals).average().orElse(Double.NaN);
    }

    // Calcola la deviazione standard campionaria rispetto alla grandMean
    public static double stdDev(String csv, double grandMean, String targetColumn) throws Exception {
        var lines = java.nio.file.Files.readAllLines(Paths.get(csv));
        int idx = Arrays.asList(lines.get(0).split(",")).indexOf(targetColumn);
        if (idx < 0) throw new IllegalArgumentException("Colonna "+targetColumn+" non trovata");

        double[] vals = lines.stream().skip(1).map(l -> l.split(","))
                .filter(a -> a.length > idx && !a[idx].isBlank())
                .mapToDouble(a -> {
                    try { return Double.parseDouble(a[idx]); } catch (Exception e) { return Double.NaN; }
                })
                .filter(d -> !Double.isNaN(d))
                .toArray();

        if (vals.length < 2) return Double.NaN;

        double variance = Arrays.stream(vals)
                .map(v -> (v - grandMean) * (v - grandMean))
                .sum() / (vals.length - 1);

        return Math.sqrt(variance);
    }

    public static void main(String[] args) throws Exception {
        String folderPath = "output/csv/";
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.contains("Respo") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("Nessun file trovato.");
            return;
        }

        Path outputPath = Paths.get(folderPath, "summaryResp.csv");
        try (CsvAppender csvAppender = new CsvAppender(outputPath, "File", "Rows", "GrandMean", "StdDev", "SemiInterval", "Inizio intervallo", "Fine intervallo")) {

            for (File file : files) {
                String csv = file.getAbsolutePath();
                int rows = countRows(csv);
                double mean = grandMean(csv, "R0captured");
                double std = stdDev(csv, mean, "R0captured");

                IntervalEstimation ie = new IntervalEstimation(0.95);
                double semiInterval = ie.semiIntervalEstimation(std, rows);

                // Debug utile per capire i valori
                System.out.printf("File: %s | Rows: %d | Mean: %.6f | StdDev: %.6f | SemiInterval: %.6f%n | Intervall: [%.6f , %.6f] ",
                        file.getName(), rows, mean, std, semiInterval, mean-semiInterval, mean+semiInterval);

                csvAppender.writeRow(
                        file.getName(),
                        String.valueOf(rows),
                        String.valueOf(mean),
                        String.valueOf(std),
                        String.valueOf(semiInterval),
                        String.valueOf(mean - semiInterval),
                        String.valueOf(mean+semiInterval)
                );
            }
        }

        System.out.println("CSV finale creato in: " + outputPath);





        files = folder.listFiles((dir, name) -> name.contains("Utilization") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("Nessun file trovato.");
            return;
        }

        outputPath = Paths.get(folderPath, "summaryUtilizationWS.csv");
        try (CsvAppender csvAppender = new CsvAppender(outputPath, "File", "Rows", "GrandMean", "StdDev", "SemiInterval")) {

            for (File file : files) {
                String csv = file.getAbsolutePath();
                int rows = countRows(csv);
                double mean = grandMean(csv, "MeanWS");
                double std = stdDev(csv, mean, "MeanWS");

                IntervalEstimation ie = new IntervalEstimation(0.95);
                double semiInterval = ie.semiIntervalEstimation(std, rows);

                // Debug utile per capire i valori
                System.out.printf("File: %s | Rows: %d | Mean: %.6f | StdDev: %.6f | SemiInterval: %.6f%n",
                        file.getName(), rows, mean, std, semiInterval);

                csvAppender.writeRow(
                        file.getName(),
                        String.valueOf(rows),
                        String.valueOf(mean),
                        String.valueOf(std),
                        String.valueOf(semiInterval)
                );
            }
        }

        System.out.println("CSV finale creato in: " + outputPath);
    }
}
