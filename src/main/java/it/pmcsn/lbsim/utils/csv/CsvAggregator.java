package it.pmcsn.lbsim.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvAggregator {

    public static void main(String[] args) {

        String tag = "WS500R0max7";
        String outputFile = tag + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {

            // ==== MeanJobs ====
            Map<String, Double> meanJobs = avgPerServer("output/csv/MeanJobs.csv", "Mean");

            // ==== ResponseR0 ====
            double responseR0 = avgSingle("output/csv/ResponseR0.csv", "Mean");

            // ==== ResponseTime ====
            Map<String, Double> respTimeMean = avgPerServer("output/csv/ResponseTime.csv", "Mean");
            Map<String, Double> respTimeRedirect = avgPerServer("output/csv/ResponseTime.csv", "%reqDirected");
            Map<String, Double> respTimeThroughput = avgPerServer("output/csv/ResponseTime.csv", "Throughput");

            // ==== Utilization ====
            Map<String, Double> utilization = avgPerServer("output/csv/Utilization.csv", "Mean");

            // ==== Scrittura header ====
            List<String> header = new ArrayList<>();
            header.add("ResponseR0");

            // MeanJobs
            for (String sid : meanJobs.keySet()) {
                header.add("MeanJobs_Server" + sid);
            }
            // ResponseTime
            for (String sid : respTimeMean.keySet()) {
                header.add("RespTimeMean_Server" + sid);
                header.add("RespTimeRedirect_Server" + sid);
                header.add("RespTimeThroughput_Server" + sid);
            }
            // Utilization
            for (String sid : utilization.keySet()) {
                header.add("Utilization_Server" + sid);
            }

            writer.println(String.join(",", header));

            // ==== Scrittura valori ====
            List<String> values = new ArrayList<>();
            values.add(String.format(Locale.US, "%.6f", responseR0));

            for (String sid : meanJobs.keySet()) {
                values.add(String.format(Locale.US, "%.6f", meanJobs.get(sid)));
            }
            for (String sid : respTimeMean.keySet()) {
                values.add(String.format(Locale.US, "%.6f", respTimeMean.get(sid)));
                values.add(String.format(Locale.US, "%.6f", respTimeRedirect.get(sid)));
                values.add(String.format(Locale.US, "%.6f", respTimeThroughput.get(sid)));
            }
            for (String sid : utilization.keySet()) {
                values.add(String.format(Locale.US, "%.6f", utilization.get(sid)));
            }

            writer.println(String.join(",", values));
            System.out.println("File creato: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =================== HELPERS ===================

    // media di una colonna (indipendente da serverID)
    private static double avgSingle(String filename, String column) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        if (lines.isEmpty()) return 0.0;

        String[] header = lines.get(0).split(",");
        int idx = findIndex(header, column);

        return lines.stream().skip(1)
                .map(l -> l.split(","))
                .filter(p -> p.length > idx && !p[idx].trim().isEmpty())
                .mapToDouble(p -> Double.parseDouble(p[idx].trim().replace("\"", "")))
                .average().orElse(0.0);
    }


    // media di una colonna per ogni ServerID
    private static Map<String, Double> avgPerServer(String filename, String column) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        if (lines.isEmpty()) return Collections.emptyMap();

        String[] header = lines.get(0).split(",");
        int idxServer = findIndex(header, "ServerID");
        int idxVal = findIndex(header, column);

        Map<String, List<Double>> map = new HashMap<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            if (parts.length <= idxVal) continue;

            String sid = parts[idxServer].trim().replace("\"", "");
            String rawVal = parts[idxVal].trim().replace("\"", "");

            if (!rawVal.isEmpty()) {
                double val = Double.parseDouble(rawVal);
                map.computeIfAbsent(sid, k -> new ArrayList<>()).add(val);
            }
        }

        Map<String, Double> result = new TreeMap<>();
        for (Map.Entry<String, List<Double>> e : map.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.put(e.getKey(), avg);
        }
        return result;
    }


    // trova indice colonna
    private static int findIndex(String[] header, String column) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(column)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Colonna non trovata: " + column);
    }
}
