package it.pmcsn.lbsim.utils;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

public class AutocorrelationCalculator {

    public static double lag1Autocorrelation(List<Double> values) {
        if (values.size() < 2) return Double.NaN;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double num = 0.0, den = 0.0;
        for (int t = 1; t < values.size(); t++) {
            num += (values.get(t) - mean) * (values.get(t - 1) - mean);
        }
        for (double v : values) {
            den += Math.pow(v - mean, 2);
        }

        return num / den;
    }

    /**
     * Legge i valori della colonna "Mean" filtrando per ServerID.
     */
    public static List<Double> readMeansFromCsv(Path filePath, String separator, int serverId) throws IOException {
        List<Double> means = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String header = br.readLine();
            if (header == null) return means;

            String[] columns = header.split(separator);
            int meanIndex = -1, serverIndex = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].trim().equalsIgnoreCase("Mean")) {
                    meanIndex = i;
                }
                if (columns[i].trim().equalsIgnoreCase("ServerID")) {
                    serverIndex = i;
                }
            }
            if (meanIndex == -1) {
                throw new IllegalArgumentException("Colonna Mean non trovata in " + filePath);
            }
            if (serverIndex == -1) {
                throw new IllegalArgumentException("Colonna ServerID non trovata in " + filePath);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] tokens = line.split(separator);
                try {
                    int id = Integer.parseInt(tokens[serverIndex].trim());
                    if (id == serverId) {
                        double val = Double.parseDouble(tokens[meanIndex]);
                        means.add(val);
                    }
                } catch (Exception e) {
                    // ignora righe malformate
                }
            }
        }
        return means;
    }

    /**
     * Trova il massimo ServerID esistente (escluso lo SpikeServer = -1).
     */
    public static int findMaxServerId(Path filePath, String separator) throws IOException {
        Set<Integer> ids = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String header = br.readLine();
            if (header == null) return -1;

            String[] columns = header.split(separator);
            int serverIndex = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].trim().equalsIgnoreCase("ServerID")) {
                    serverIndex = i;
                    break;
                }
            }
            if (serverIndex == -1) {
                throw new IllegalArgumentException("Colonna ServerID non trovata in " + filePath);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] tokens = line.split(separator);
                try {
                    int id = Integer.parseInt(tokens[serverIndex].trim());
                    if (id >= 0) ids.add(id);
                } catch (Exception e) {
                    // ignora righe malformate
                }
            }
        }

        // Trova il primo buco nella sequenza [0..N]
        int max = 0;
        while (ids.contains(max)) {
            max++;
        }
        return max; // il primo che NON esiste
    }

    private static void printAndWriteReport(PrintWriter writer, String centerName,
                                            double rhoResp, double rhoUtil, double rhoJobs,
                                            int b, int k, String fileName1, String fileName2, String fileName3) {
        String separator = "****************************************";
        String dataLine = "Data for " + fileName1 + ", " + fileName2 + ", " + fileName3;
        String titleLine = String.format("AUTOCORRELATION VALUES FOR %s [B:%d|K:%d]", centerName, b, k);
        String respLine = String.format("E[ResponseTime]: %.4f", rhoResp);
        String utilLine = String.format("E[Utilization]:  %.4f", rhoUtil);
        String jobsLine = String.format("E[MeanJobs]:     %.4f", rhoJobs);

        // Print to console
        System.out.println(separator);
        System.out.println(dataLine);
        System.out.println(titleLine);
        System.out.println(respLine);
        System.out.println(utilLine);
        System.out.println(jobsLine);
        System.out.println(separator);
        System.out.println();

        // Write to file
        writer.println(separator);
        writer.println(dataLine);
        writer.println(titleLine);
        writer.println(respLine);
        writer.println(utilLine);
        writer.println(jobsLine);
        writer.println(separator);
        writer.println();
    }

    public static void main(String[] args) throws IOException {
        final String configFilePath = "config.yaml"; // Default configuration file path
        SimConfiguration config = ConfigLoader.load(configFilePath);
        int b = config.getBatchSize();
        int k = config.getNumberOfBatchs();
        int si = config.getSImax();

        // File CSV
        Path responseTimeCsv = Path.of("output/csv/ResponseTimeSI160.csv");
        Path utilizationCsv   = Path.of("output/csv/UtilizationSI160.csv");
        Path meanJobsCsv      = Path.of("output/csv/MeanJobsSI160.csv");

        // Crea la directory output se non esiste
        Path outputDir = Path.of("output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Crea il nome del file di output con Si, b, k
        String outputFileName = String.format("AutocorrelationReport_SI%d_B%d_K%d.txt",si, b, k);
        Path outputFile = outputDir.resolve(outputFileName);

        // Trova il numero di WebServer (0..N-1)
        int maxServerId = findMaxServerId(responseTimeCsv, ",");

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile.toFile()))) {
            // Header del file
            writer.println("AUTOCORRELATION ANALYSIS REPORT");
            writer.println("Generated on: " + java.time.LocalDateTime.now());
            writer.println("Configuration: SI=" + si + ", BatchSize=" + b + ", NumberOfBatchs=" + k);
            writer.println("Source files: " + responseTimeCsv.getFileName() + ", " +
                    utilizationCsv.getFileName() + ", " + meanJobsCsv.getFileName());
            writer.println();

            // SpikeServer (-1)
            double rhoRespSpike = lag1Autocorrelation(readMeansFromCsv(responseTimeCsv, ",", -1));
            double rhoUtilSpike = lag1Autocorrelation(readMeansFromCsv(utilizationCsv, ",", -1));
            double rhoJobsSpike = lag1Autocorrelation(readMeansFromCsv(meanJobsCsv, ",", -1));
            printAndWriteReport(writer, "SpikeServer", rhoRespSpike, rhoUtilSpike, rhoJobsSpike, b, k,
                    responseTimeCsv.getFileName().toString(),
                    utilizationCsv.getFileName().toString(),
                    meanJobsCsv.getFileName().toString());

            // WebServer (0..maxServerId-1)
            for (int id = 0; id < maxServerId; id++) {
                double rhoResp = lag1Autocorrelation(readMeansFromCsv(responseTimeCsv, ",", id));
                double rhoUtil = lag1Autocorrelation(readMeansFromCsv(utilizationCsv, ",", id));
                double rhoJobs = lag1Autocorrelation(readMeansFromCsv(meanJobsCsv, ",", id));
                printAndWriteReport(writer, "WebServer" + id, rhoResp, rhoUtil, rhoJobs, b, k,
                        responseTimeCsv.getFileName().toString(),
                        utilizationCsv.getFileName().toString(),
                        meanJobsCsv.getFileName().toString());
            }

            System.out.println("Report saved to: " + outputFile.toAbsolutePath());
        }
    }
}