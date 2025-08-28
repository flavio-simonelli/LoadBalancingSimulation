
package it.pmcsn.lbsim.utils.plot;

import com.opencsv.CSVReader;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class PlotCSV {

    private PlotCSV() {
        // Static class
    }

    public static void plotScatter(String csvPath, String plotDir, String xCol, String yCol) throws Exception {
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            String[] header = reader.readNext();
            if (header == null) throw new IllegalArgumentException("empty CSV");

            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colMap.put(header[i], i);
            }
            Integer xIdx = colMap.get(xCol);
            Integer yIdx = colMap.get(yCol);
            if (xIdx == null || yIdx == null) throw new IllegalArgumentException("Columns not found");

            String[] row;
            while ((row = reader.readNext()) != null) {
                xData.add(Double.parseDouble(row[xIdx]));
                yData.add(Double.parseDouble(row[yIdx]));
            }
        }

        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title(xCol + " vs " + yCol)
                .xAxisTitle(xCol)
                .yAxisTitle(yCol)
                .build();

        chart.getStyler().setMarkerSize(1);
        chart.getStyler().setPlotGridLinesVisible(false);

        XYSeries series = chart.addSeries(xCol + " vs " + yCol, xData, yData);
        series.setMarker(SeriesMarkers.CIRCLE);
        series.setMarkerColor(Color.BLACK);
        series.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.NONE);

        // Highlight min and max Y points in red
        int minIdx = 0, maxIdx = 0;
        for (int i = 1; i < yData.size(); i++) {
            if (yData.get(i) < yData.get(minIdx)) minIdx = i;
            if (yData.get(i) > yData.get(maxIdx)) maxIdx = i;
        }

        XYSeries minSeries = chart.addSeries("Min " + yCol,
                Collections.singletonList(xData.get(minIdx)),
                Collections.singletonList(yData.get(minIdx)));
        minSeries.setMarker(SeriesMarkers.CIRCLE);
        minSeries.setMarkerColor(Color.RED);
        minSeries.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.NONE);

        XYSeries maxSeries = chart.addSeries("Max " + yCol,
                Collections.singletonList(xData.get(maxIdx)),
                Collections.singletonList(yData.get(maxIdx)));
        maxSeries.setMarker(SeriesMarkers.CIRCLE);
        maxSeries.setMarkerColor(Color.RED);
        maxSeries.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.NONE);

        String csvFileName = Paths.get(csvPath).getFileName().toString().replaceAll("\\.csv$", "");
        String outputFileName = csvFileName + "_" + xCol + "_VS_" + yCol + ".svg";
        Path outputPath = Paths.get(plotDir, outputFileName);

        Files.createDirectories(outputPath.getParent());

        VectorGraphicsEncoder.saveVectorGraphic(chart, outputPath.toString(), VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
    }
}