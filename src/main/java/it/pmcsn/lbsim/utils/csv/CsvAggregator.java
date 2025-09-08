package it.pmcsn.lbsim.utils.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import it.pmcsn.lbsim.utils.IntervalEstimation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CsvAggregator {

    private static final IntervalEstimation intervalEstimation = new IntervalEstimation(0.95);

    public static void main(String[] args) throws CsvValidationException {
        String tag = "HorizontalScalingStudy";
        String outputFile = "output/csv/" + tag + ".csv";

        try {
            boolean exists = Files.exists(Path.of(outputFile));

            // ==== MeanJobs ====
            Map<String, Stats> meanJobs = statsPerServer("output/csv/MeanJobs.csv", "Mean");

            // ==== ResponseR0 ====
            Stats responseR0 = statsSingle("output/csv/ResponseR0.csv", "Mean");

            // ==== ResponseTime ====
            Map<String, Stats> respTimeMean = statsPerServer("output/csv/ResponseTime.csv", "Mean");
            Map<String, Stats> respTimeRedirect = statsPerServer("output/csv/ResponseTime.csv", "%reqDirected");
            Map<String, Stats> respTimeThroughput = statsPerServer("output/csv/ResponseTime.csv", "Throughput");

            // ==== Utilization ====
            Map<String, Stats> utilization = statsPerServer("output/csv/Utilization.csv", "Mean");

            // ==== Costruzione header dinamico ====
            List<String> header = new ArrayList<>();
            // Colonne extra hard-coded
            header.add("R0max");       // ExtraCol1
            header.add("WindowSize"); // ExtraCol2
            header.add("LambdaInterarrivi"); // ExtraCol3
            // Colonne statistiche
            header.add("ResponseR0_Mean");
            header.add("ResponseR0_SemiInt");

            for (String sid : meanJobs.keySet()) {
                header.add("MeanJobs_Server" + sid + "_Mean");
                header.add("MeanJobs_Server" + sid + "_SemiInt");
            }
            for (String sid : respTimeMean.keySet()) {
                header.add("RespTimeMean_Server" + sid + "_Mean");
                header.add("RespTimeMean_Server" + sid + "_SemiInt");
                header.add("RespTimeRedirect_Server" + sid + "_Mean");
                header.add("RespTimeRedirect_Server" + sid + "_SemiInt");
                header.add("RespTimeThroughput_Server" + sid + "_Mean");
                header.add("RespTimeThroughput_Server" + sid + "_SemiInt");
            }
            for (String sid : utilization.keySet()) {
                header.add("Utilization_Server" + sid + "_Mean");
                header.add("Utilization_Server" + sid + "_SemiInt");
            }

            // ==== Costruzione riga valori ====
            List<String> values = new ArrayList<>();
            // Valori extra hard-coded
            values.add("8");        // ExtraCol1
            values.add("1500");    // ExtraCol2
            values.add("9");  // ExtraCol3
            values.add(String.format(Locale.US, "%.6f", responseR0.mean));
            values.add(String.format(Locale.US, "%.6f",
                    intervalEstimation.semiIntervalEstimation(responseR0.stddev, responseR0.n)));

            for (String sid : meanJobs.keySet()) {
                Stats st = meanJobs.get(sid);
                values.add(String.format(Locale.US, "%.6f", st.mean));
                values.add(String.format(Locale.US, "%.6f",
                        intervalEstimation.semiIntervalEstimation(st.stddev, st.n)));
            }
            for (String sid : respTimeMean.keySet()) {
                Stats m = respTimeMean.get(sid);
                Stats r = respTimeRedirect.get(sid);
                Stats t = respTimeThroughput.get(sid);

                values.add(String.format(Locale.US, "%.6f", m.mean));
                values.add(String.format(Locale.US, "%.6f",
                        intervalEstimation.semiIntervalEstimation(m.stddev, m.n)));

                values.add(String.format(Locale.US, "%.6f", r.mean));
                values.add(String.format(Locale.US, "%.6f",
                        intervalEstimation.semiIntervalEstimation(r.stddev, r.n)));

                values.add(String.format(Locale.US, "%.6f", t.mean));
                values.add(String.format(Locale.US, "%.6f",
                        intervalEstimation.semiIntervalEstimation(t.stddev, t.n)));
            }
            for (String sid : utilization.keySet()) {
                Stats st = utilization.get(sid);
                values.add(String.format(Locale.US, "%.6f", st.mean));
                values.add(String.format(Locale.US, "%.6f",
                        intervalEstimation.semiIntervalEstimation(st.stddev, st.n)));
            }

            // ==== Scrittura su file ====
            if (!exists) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
                    writer.println(String.join(",", header));
                }
            } else {
                // Controllo header
                try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
                    String firstLine = br.readLine();
                    String expectedHeader = String.join(",", header);
                    if (firstLine == null || !firstLine.equals(expectedHeader)) {
                        throw new IllegalStateException("Header non corrisponde a quello atteso in " + outputFile);
                    }
                }
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
                writer.println(String.join(",", values));
            }

            System.out.println("Riga aggiunta al file: " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =================== HELPERS ===================

    static class Stats {
        double mean;
        double stddev;
        int n;

        Stats(List<Double> values) {
            this.n = values.size();
            if (n <= 1) {
                mean = stddev = 0.0;
            } else {
                mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / (n - 1);
                stddev = Math.sqrt(variance);
            }
        }
    }

    private static Stats statsSingle(String filename, String column) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] header = reader.readNext();
            int idx = findIndex(header, column);

            List<Double> values = new ArrayList<>();
            String[] row;
            while ((row = reader.readNext()) != null) {
                String rawVal = row[idx].trim();
                if (!rawVal.isEmpty()) {
                    values.add(Double.parseDouble(rawVal));
                }
            }
            return new Stats(values);
        }
    }

    private static Map<String, Stats> statsPerServer(String filename, String column) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] header = reader.readNext();
            int idxServer = findIndex(header, "ServerID");
            int idxVal = findIndex(header, column);

            Map<String, List<Double>> map = new HashMap<>();
            String[] row;
            while ((row = reader.readNext()) != null) {
                String sid = row[idxServer].trim();
                String rawVal = row[idxVal].trim();

                if (!rawVal.isEmpty()) {
                    double val = Double.parseDouble(rawVal);
                    map.computeIfAbsent(sid, k -> new ArrayList<>()).add(val);
                }
            }

            Map<String, Stats> result = new TreeMap<>();
            for (Map.Entry<String, List<Double>> e : map.entrySet()) {
                result.put(e.getKey(), new Stats(e.getValue()));
            }
            return result;
        }
    }

    private static int findIndex(String[] header, String column) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(column)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Colonna non trovata: " + column);
    }
}
