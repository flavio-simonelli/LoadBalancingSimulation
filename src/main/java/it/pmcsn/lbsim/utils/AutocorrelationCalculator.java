package it.pmcsn.lbsim.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
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

    private static void printReport(String centerName,
                                    double rhoResp, double rhoUtil, double rhoJobs) {
        System.out.println("****************************************");
        System.out.printf("AUTOCORRELATION VALUES FOR %s [B:10000|K:256]%n", centerName);
        System.out.printf("E[ResponseTime]: %.4f%n", rhoResp);
        System.out.printf("E[Utilization]:  %.4f%n", rhoUtil);
        System.out.printf("E[MeanJobs]:     %.4f%n", rhoJobs);
        System.out.println("****************************************");
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        // File CSV
        Path responseTimeCsv = Path.of("output/csv/ResponseTime.csv");
        Path utilizationCsv   = Path.of("output/csv/Utilization.csv");
        Path meanJobsCsv      = Path.of("output/csv/MeanJobs.csv");

        // Trova il numero di WebServer (0..N-1)
        int maxServerId = findMaxServerId(responseTimeCsv, ",");

        // SpikeServer (-1)
        double rhoRespSpike = lag1Autocorrelation(readMeansFromCsv(responseTimeCsv, ",", -1));
        double rhoUtilSpike = lag1Autocorrelation(readMeansFromCsv(utilizationCsv, ",", -1));
        double rhoJobsSpike = lag1Autocorrelation(readMeansFromCsv(meanJobsCsv, ",", -1));
        printReport("SpikeServer", rhoRespSpike, rhoUtilSpike, rhoJobsSpike);

        // WebServer (0..maxServerId-1)
        for (int id = 0; id < maxServerId; id++) {
            double rhoResp = lag1Autocorrelation(readMeansFromCsv(responseTimeCsv, ",", id));
            double rhoUtil = lag1Autocorrelation(readMeansFromCsv(utilizationCsv, ",", id));
            double rhoJobs = lag1Autocorrelation(readMeansFromCsv(meanJobsCsv, ",", id));
            printReport("WebServer" + id, rhoResp, rhoUtil, rhoJobs);
        }
    }
}
