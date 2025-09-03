package it.pmcsn.lbsim.utils;

import it.pmcsn.lbsim.utils.random.Rvms;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntervalEstimation {
    private final double LOC; // Level Of Confidence
    private final Rvms rvms;

    public IntervalEstimation(double LOC) {
        if (LOC <= 0.0 || LOC >= 1.0) {
            throw new IllegalArgumentException("LOC must be in (0,1)");
        }
        this.LOC = LOC;
        this.rvms = new Rvms();
    }

    public double semiIntervalEstimation(double standardDeviation, int n) {
        double u = 1.0 - 0.5 * (1.0 - LOC);

        double t;
        if (n > 2500000) {
            // approssima con quantile normale
            t = rvms.idfNormal(0.0, 1.0, u);  // 1.96 per 95%
        } else {
            t = rvms.idfStudent(n - 1, u);
        }

        return t * standardDeviation / Math.sqrt(n-1);
    }


    /**
     * Calcola l'intervallo di confidenza da un file CSV contenente i risultati delle repliche
     * @param csvFilePath percorso del file CSV
     * @param jobsColumnIndex indice della colonna contenente il numero di job per replica (0-based)
     * @param meanColumnIndex indice della colonna contenente la media calcolata per replica (0-based)
     * @param hasHeader true se il CSV ha una riga di header da saltare
     * @return oggetto ConfidenceIntervalResult contenente i risultati
     * @throws IOException se ci sono errori nella lettura del file
     */
    public ConfidenceIntervalResult calculateConfidenceIntervalFromCSV(
            String csvFilePath,
            int jobsColumnIndex,
            int meanColumnIndex,
            boolean hasHeader) throws IOException {

        List<Integer> jobCounts = new ArrayList<>();
        List<Double> means = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Salta la riga di header se presente
                if (hasHeader && firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] fields = line.split(",");

                try {
                    int jobs = Integer.parseInt(fields[jobsColumnIndex].trim());
                    double mean = Double.parseDouble(fields[meanColumnIndex].trim());

                    jobCounts.add(jobs);
                    means.add(mean);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.err.println("Errore nel parsing della riga: " + line);
                    // Continua con la riga successiva
                }

                firstLine = false;
            }
        }

        if (means.isEmpty()) {
            throw new IllegalArgumentException("Nessun dato valido trovato nel file CSV");
        }

        return calculateConfidenceInterval(means, jobCounts);
    }

    /**
     * Calcola l'intervallo di confidenza da liste di medie e conteggi di job
     */
    private ConfidenceIntervalResult calculateConfidenceInterval(List<Double> means, List<Integer> jobCounts) {
        int n = means.size(); // numero di repliche

        // Calcola la media delle medie (grand mean)
        double grandMean = means.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calcola la varianza campionaria delle medie
        double variance = means.stream()
                .mapToDouble(mean -> Math.pow(mean - grandMean, 2))
                .sum() / (n - 1);

        double standardDeviation = Math.sqrt(variance);

        // Calcola il semi-intervallo
        double semiInterval = semiIntervalEstimation(standardDeviation, n);

        // Calcola i limiti dell'intervallo
        double lowerBound = grandMean - semiInterval;
        double upperBound = grandMean + semiInterval;

        // Calcola statistiche aggiuntive
        int totalJobs = jobCounts.stream().mapToInt(Integer::intValue).sum();
        double avgJobsPerReplica = jobCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        return new ConfidenceIntervalResult(
                grandMean,
                lowerBound,
                upperBound,
                semiInterval,
                standardDeviation,
                n,
                totalJobs,
                avgJobsPerReplica,
                LOC
        );
    }

    /**
     * Classe per contenere i risultati del calcolo dell'intervallo di confidenza
     */
    public static class ConfidenceIntervalResult {
        public final double grandMean;           // Media delle medie
        public final double lowerBound;          // Limite inferiore
        public final double upperBound;          // Limite superiore
        public final double semiInterval;        // Semi-intervallo
        public final double standardDeviation;   // Deviazione standard
        public final int numberOfReplicas;       // Numero di repliche
        public final int totalJobs;              // Totale job processati
        public final double avgJobsPerReplica;   // Media job per replica
        public final double confidenceLevel;     // Livello di confidenza

        public ConfidenceIntervalResult(double grandMean, double lowerBound, double upperBound,
                                        double semiInterval, double standardDeviation,
                                        int numberOfReplicas, int totalJobs,
                                        double avgJobsPerReplica, double confidenceLevel) {
            this.grandMean = grandMean;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.semiInterval = semiInterval;
            this.standardDeviation = standardDeviation;
            this.numberOfReplicas = numberOfReplicas;
            this.totalJobs = totalJobs;
            this.avgJobsPerReplica = avgJobsPerReplica;
            this.confidenceLevel = confidenceLevel;
        }

        @Override
        public String toString() {
            return String.format(
                    "Intervallo di Confidenza (%.1f%%):\n" +
                            "  Media delle medie: %.6f\n" +
                            "  Intervallo: [%.6f, %.6f]\n" +
                            "  Semi-intervallo: ±%.6f\n" +
                            "  Deviazione standard: %.6f\n" +
                            "  Numero repliche: %d\n" +
                            "  Totale job: %d\n" +
                            "  Media job per replica: %.1f",
                    confidenceLevel * 100,
                    grandMean,
                    lowerBound, upperBound,
                    semiInterval,
                    standardDeviation,
                    numberOfReplicas,
                    totalJobs,
                    avgJobsPerReplica
            );
        }
    }

    /**
     * Metodo main per testare il calcolo dell'intervallo di confidenza
     * Genera il CSV aggregato dai file Welford e calcola l'intervallo
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== GENERAZIONE CSV AGGREGATI PER GRUPPI jN ===");

            // Elenco file Welford disponibili
            java.util.List<String> welfordFiles = it.pmcsn.lbsim.utils.csv.CsvAppender.listWelfordFiles("output/csv/");
            System.out.println("File Welford trovati (" + welfordFiles.size() + "):");
            for (String file : welfordFiles) {
                System.out.println("  - " + file);
            }

            if (welfordFiles.isEmpty()) {
                System.err.println("⚠️  Nessun file Welford trovato in output/csv/");
                return;
            }

            // Raggruppa i file per jN
            java.util.Map<String, java.util.List<String>> groupedFiles = new java.util.HashMap<>();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("j(\\d+)");

            for (String file : welfordFiles) {
                java.util.regex.Matcher matcher = pattern.matcher(file);
                if (matcher.find()) {
                    String groupKey = matcher.group(); // es. "j0", "j1", "j2"
                    groupedFiles.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(file);
                } else {
                    groupedFiles.computeIfAbsent("ungrouped", k -> new java.util.ArrayList<>()).add(file);
                }
            }

            // Estimatore con confidenza 95%
            IntervalEstimation estimator = new IntervalEstimation(0.95);

            // Percorso della cartella dei CSV
            java.nio.file.Path csvDir = java.nio.file.Paths.get("output/csv/");

            // Processa ogni gruppo
            for (var entry : groupedFiles.entrySet()) {
                String groupKey = entry.getKey();
                java.util.List<String> files = entry.getValue();

                System.out.println("\n=== Gruppo: " + groupKey + " (" + files.size() + " file) ===");

                // Cartella backup temporanea
                java.nio.file.Path backupDir = java.nio.file.Files.createTempDirectory("backup_" + groupKey);

                // Sposta TUTTI i file in backup
                java.util.List<java.nio.file.Path> allFiles = java.nio.file.Files.list(csvDir).toList();
                for (java.nio.file.Path f : allFiles) {
                    java.nio.file.Files.move(f, backupDir.resolve(f.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // Riporta solo i file del gruppo
                for (String file : files) {
                    java.nio.file.Path source = backupDir.resolve(file);
                    java.nio.file.Path target = csvDir.resolve(file);
                    java.nio.file.Files.move(source, target,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // Path CSV aggregato per il gruppo
                String aggregatedCsvPath = "output/aggregated_response_times_" + groupKey + ".csv";

                // Ora CsvAppender vede SOLO i file del gruppo
                it.pmcsn.lbsim.utils.csv.CsvAppender.createResponseTimeAggregatedCsv(
                        java.nio.file.Paths.get(aggregatedCsvPath)
                );

                System.out.println("✅ CSV aggregato creato: " + aggregatedCsvPath);

                // Calcola intervallo di confidenza
                ConfidenceIntervalResult result = estimator.calculateConfidenceIntervalFromCSV(
                        aggregatedCsvPath,
                        1, // colonna jobs_count
                        2, // colonna mean_response_time
                        true // ha header
                );

                System.out.println("✅ Calcolo completato!\n");
                System.out.println(result);

                // Analisi della precisione
                double relativeError = (result.semiInterval / result.grandMean) * 100;
                System.out.printf("📏 Errore relativo: ±%.2f%%\n", relativeError);

                if (relativeError < 5) {
                    System.out.println("✅ Ottima precisione! (errore < 5%)");
                } else if (relativeError < 10) {
                    System.out.println("👍 Buona precisione (errore < 10%)");
                } else {
                    System.out.println("⚠️  Precisione moderata - considera più repliche per migliorare");
                }

                // Ripristina i file originali
                java.util.List<java.nio.file.Path> tempFiles = java.nio.file.Files.list(csvDir).toList();
                for (java.nio.file.Path f : tempFiles) {
                    java.nio.file.Files.move(f, backupDir.resolve(f.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                for (java.nio.file.Path f : java.nio.file.Files.list(backupDir).toList()) {
                    java.nio.file.Files.move(f, csvDir.resolve(f.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

        } catch (java.io.IOException e) {
            System.err.println("❌ Errore I/O: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Errore nei dati: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Errore generico: " + e.getMessage());
            e.printStackTrace();
        }
    }


}