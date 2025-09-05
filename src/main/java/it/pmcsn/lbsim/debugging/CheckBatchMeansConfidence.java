package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.IntervalEstimation;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CheckBatchMeansConfidence {

    public static int countRows(String csv) throws Exception {
        return Math.toIntExact(Math.max(0, java.nio.file.Files.lines(Paths.get(csv)).count() - 1));
    }

    public static double stdDev(String csv) throws Exception {
        var lines = java.nio.file.Files.readAllLines(Paths.get(csv));
        int idx = Arrays.asList(lines.get(0).split(",")).indexOf("R0captured");
        if (idx < 0) throw new IllegalArgumentException("Colonna 'R0captured' non trovata");
        double[] vals = lines.stream().skip(1).map(l -> l.split(","))
                .filter(a -> a.length > idx && !a[idx].isBlank())
                .mapToDouble(a -> {
                    try { return Double.parseDouble(a[idx]); } catch (Exception e) { return Double.NaN; }
                })
                .filter(d -> !Double.isNaN(d)).toArray();
        double mean = Arrays.stream(vals).average().orElse(Double.NaN);
        double variance = vals.length > 1
                ? Arrays.stream(vals).map(v -> (v - mean) * (v - mean)).sum() / (vals.length - 1)
                : Double.NaN;
        return Math.sqrt(variance);
    }

    public static double grandMean(String csv) throws Exception {
        var lines = java.nio.file.Files.readAllLines(Paths.get(csv));
        int idx = Arrays.asList(lines.getFirst().split(",")).indexOf("R0captured");
        if (idx < 0) throw new IllegalArgumentException("Colonna 'R0captured' non trovata");
        double[] vals = lines.stream().skip(1).map(l -> l.split(","))
                .filter(a -> a.length > idx && !a[idx].isBlank())
                .mapToDouble(a -> {
                    try { return Double.parseDouble(a[idx]); } catch (Exception e) { return Double.NaN; }
                })
                .filter(d -> !Double.isNaN(d)).toArray();
        return Arrays.stream(vals).average().orElse(Double.NaN);
    }

    public static void main(String[] args) throws Exception {
        String folderPath = "output/csv/";
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.contains("Respo") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("Nessun file trovato.");
            return;
        }

        Path outputPath = Paths.get(folderPath, "summary.csv");
        try (CsvAppender csvAppender = new CsvAppender(outputPath, "File", "Rows", "GrandMean", "StdDev", "SemiInterval")) {

            for (File file : files) {
                String csv = file.getAbsolutePath();
                int rows = countRows(csv);
                double mean = grandMean(csv);
                double std = stdDev(csv);

                IntervalEstimation ie = new IntervalEstimation(0.95);
                double semiInterval = ie.semiIntervalEstimation(std, rows);

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
