package it.pmcsn.lbsim.utils;

import it.pmcsn.lbsim.utils.random.Rvms;
import java.io.BufferedReader;
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

        return t * standardDeviation / Math.sqrt(n);
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
            // Path per il CSV aggregato
            String aggregatedCsvPath = "output/aggregated_response_times.csv";

            System.out.println("=== GENERAZIONE CSV AGGREGATO ===");

            // Prima controlla quali file Welford sono disponibili
            java.util.List<String> welfordFiles = it.pmcsn.lbsim.utils.csv.CsvAppender.listWelfordFiles("output/csv/");
            System.out.println("File Welford trovati (" + welfordFiles.size() + "):");
            for (String file : welfordFiles) {
                System.out.println("  - " + file);
            }

            if (welfordFiles.isEmpty()) {
                System.err.println("⚠️  Nessun file Welford trovato in output/csv/");
                System.err.println("   Assicurati di aver eseguito le simulazioni prima!");
                return;
            }

            // Genera il CSV aggregato
            it.pmcsn.lbsim.utils.csv.CsvAppender.createResponseTimeAggregatedCsv(
                    java.nio.file.Paths.get(aggregatedCsvPath)
            );
            System.out.println("✅ CSV aggregato creato: " + aggregatedCsvPath);

            System.out.println("\n=== CALCOLO INTERVALLO DI CONFIDENZA ===");

            // Crea l'estimatore con livello di confidenza del 95%
            IntervalEstimation estimator = new IntervalEstimation(0.95);

            // Calcola l'intervallo di confidenza dal CSV generato
            ConfidenceIntervalResult result = estimator.calculateConfidenceIntervalFromCSV(
                    aggregatedCsvPath,
                    1, // indice colonna jobs_count
                    2, // indice colonna mean_response_time
                    true // ha header
            );

            System.out.println("✅ Calcolo completato!\n");
            System.out.println(result);

            // Informazioni aggiuntive
            System.out.println("\n=== INTERPRETAZIONE RISULTATI ===");
            System.out.printf("📊 Con il %.0f%% di confidenza, il vero tempo medio di risposta\n",
                    result.confidenceLevel * 100);
            System.out.printf("   del sistema è compreso tra %.6f e %.6f secondi.\n",
                    result.lowerBound, result.upperBound);
            System.out.printf("🎯 La nostra migliore stima è: %.6f ± %.6f secondi\n",
                    result.grandMean, result.semiInterval);

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