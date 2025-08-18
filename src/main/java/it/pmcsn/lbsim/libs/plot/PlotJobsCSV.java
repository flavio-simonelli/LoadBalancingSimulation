package it.pmcsn.lbsim.libs.plot;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.*;
import java.nio.file.*;
import java.util.*;

//TODO: abbiamo degli spike troppo grossi sui tempi di risposta. Usiamo i plot per fare tuning dei parametri e delle politiche.
public class PlotJobsCSV {

    public static void main(String[] args) throws IOException {
        Path csvPath = Path.of("target/Jobs.csv");

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

        // Serie rossa (currentSi >= 10)
        XYSeries seriesTrue = chart.addSeries("CurrentSi >= 10", arrivalTimesTrue, responseTimesTrue);
        seriesTrue.setMarker(SeriesMarkers.CIRCLE);
        seriesTrue.setMarkerColor(java.awt.Color.RED);

        // Mostra grafico
        new SwingWrapper<>(chart).displayChart();
    }
}
