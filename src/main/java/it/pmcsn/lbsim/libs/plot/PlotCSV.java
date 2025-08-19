package it.pmcsn.lbsim.libs.plot;

import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.*;
import java.nio.file.*;
import java.util.*;

//TODO: abbiamo degli spike troppo grossi sui tempi di risposta. Usiamo i plot per fare tuning dei parametri e delle politiche.
public class PlotCSV {

    public static void main(String[] args) throws IOException {
        //plotXarrivalYrt();
        plotXarrivalYsize();
    }

    private static void plotXarrivalYsize() throws IOException {
        // Percorso del CSV hardcoded
        Path csvPath = Path.of("results/Jobs.csv");

        // Liste per separare isSpike true/false
        List<Double> arrivalTimesSpike = new ArrayList<>();
        List<Double> sizesSpike = new ArrayList<>();
        List<Double> arrivalTimesNormal = new ArrayList<>();
        List<Double> sizesNormal = new ArrayList<>();

        // Legge il CSV
        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            String line;

            // Salta l'header
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (values.length < 6) continue; // sicurezz

                double arrivalTime = Double.parseDouble(values[1].trim());
                double size = Double.parseDouble(values[4].trim());
                boolean isSpike = Boolean.parseBoolean(values[5].trim());

                if (isSpike) {
                    arrivalTimesSpike.add(arrivalTime);
                    sizesSpike.add(size);
                } else {
                    arrivalTimesNormal.add(arrivalTime);
                    sizesNormal.add(size);
                }
            }
        }

        // Crea il grafico scatter
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Size vs Arrival Time")
                .xAxisTitle("Arrival Time")
                .yAxisTitle("Size")
                .build();

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setMarkerSize(4);

        // Serie blu per isSpike=false
        if (!arrivalTimesNormal.isEmpty()) {
            XYSeries seriesNormal = chart.addSeries("isSpike = false", arrivalTimesNormal, sizesNormal);
            seriesNormal.setMarker(SeriesMarkers.CIRCLE);
            seriesNormal.setMarkerColor(java.awt.Color.BLUE);
        }

        // Serie rossa per isSpike=true
        if (!arrivalTimesSpike.isEmpty()) {
            XYSeries seriesSpike = chart.addSeries("isSpike = true", arrivalTimesSpike, sizesSpike);
            seriesSpike.setMarker(SeriesMarkers.CIRCLE);
            seriesSpike.setMarkerColor(java.awt.Color.RED);
        }

        // Mostra grafico
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        javax.swing.JFrame frame = sw.displayChart();
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();

        // Salva grafico in PNG dentro target/
        try {
            Files.createDirectories(Path.of("target"));
            Path outputPath = Path.of("target/size_vs_arrival.png");
            BitmapEncoder.saveBitmap(chart, outputPath.toString(), BitmapEncoder.BitmapFormat.PNG);
            System.out.println("Grafico salvato in: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    private static void plotXarrivalYrt() throws IOException {
        Path csvPath = Path.of("results/Jobs.csv");

        List<Double> arrivalTimesTrue = new ArrayList<>();
        List<Double> responseTimesTrue = new ArrayList<>();
        List<Double> arrivalTimesFalse = new ArrayList<>();
        List<Double> responseTimesFalse = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                // Salta l'header se esiste
                if (values[0].equalsIgnoreCase("jobId")) continue;

                double arrivalTime = Double.parseDouble(values[1]);
                double responseTime = Double.parseDouble(values[3]);
                boolean currentSiHigh = Boolean.parseBoolean(values[4]);

                if (currentSiHigh) { // true → rosso
                    arrivalTimesTrue.add(arrivalTime);
                    responseTimesTrue.add(responseTime);
                } else { // false → blu
                    arrivalTimesFalse.add(arrivalTime);
                    responseTimesFalse.add(responseTime);
                }
            }
        }

        // Crea grafico scatter
        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title("Response Time vs Arrival Time")
                .xAxisTitle("Arrival Time")
                .yAxisTitle("Response Time")
                .build();

        // Stile globale per puntini piccoli
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setMarkerSize(4); // molto piccoli

        // Serie blu (currentSi < 10)
        XYSeries seriesFalse = chart.addSeries("CurrentSi < 10", arrivalTimesFalse, responseTimesFalse);
        seriesFalse.setMarker(SeriesMarkers.CIRCLE);
        seriesFalse.setMarkerColor(java.awt.Color.BLUE);

        // Mostra grafico
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        javax.swing.JFrame frame = sw.displayChart();

        frame.setAlwaysOnTop(true);     // la finestra resta sempre davanti
        frame.toFront();                // porta davanti
        frame.requestFocus();           // prova a dargli il focus
    }

    private static void showChart(XYChart chart) {
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        javax.swing.JFrame frame = sw.displayChart();

        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
    }
}
