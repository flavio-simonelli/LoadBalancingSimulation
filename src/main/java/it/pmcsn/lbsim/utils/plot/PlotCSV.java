package it.pmcsn.lbsim.utils.plot;

import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.config.YamlSimulationConfig;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for plotting CSV data from simulation results.
 * Creates scatter plots to analyze job arrival times, sizes, and response times.
 */
public class PlotCSV {
    private static final Logger logger = Logger.getLogger(PlotCSV.class.getName());
    private static final String CONFIG_FILE_PATH = "config.yaml";
    private static final int CHART_WIDTH = 1200;
    private static final int CHART_HEIGHT = 800;
    private static final int MARKER_SIZE = 2;

    private final Path csvPath;

    public PlotCSV() {
        SimConfiguration config = new YamlSimulationConfig(CONFIG_FILE_PATH);
        this.csvPath = Path.of(config.getCsvJobsPath());
    }

    public static void main(String[] args) throws IOException {
        PlotCSV plotter = new PlotCSV();
        plotter.plotSizeVsArrivalTime();
        plotter.plotIdVsSize();
        plotter.plotResponseTimeVsArrivalTime();
    }

    public void plotIdVsSize() throws IOException {
        List<Double> ids = new ArrayList<>();
        List<Double> sizes = new ArrayList<>();

        // Modifica qui il path se necessario
        Path csvPath = Path.of("results/genArrivals.csv");

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            // Salta header
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2) continue;
                ids.add(Double.parseDouble(values[0].trim()));
                sizes.add(Double.parseDouble(values[1].trim()));
            }
        }

        XYChart chart = createChart("ID vs Size", "ID", "Size");
        addSeries(chart, "Job Size", ids, sizes, Color.BLUE);
        displayAndSaveChart(chart, "id_vs_size.svg");
    }

    /**
     * Creates a scatter plot showing job size vs arrival time,
     * distinguishing between spike and normal jobs.
     */
    public void plotSizeVsArrivalTime() throws IOException {
        JobData jobData = readJobData();

        XYChart chart = createChart("Size vs Arrival Time", "Arrival Time", "Size");

        // Add series for normal jobs (blue with transparency)
        if (!jobData.normalArrivalTimes.isEmpty()) {
            addSeries(chart, "Normal Jobs (" + jobData.normalArrivalTimes.size() + ")",
                    jobData.normalArrivalTimes, jobData.normalSizes, new Color(0, 100, 200));
        }

        // Add series for spike jobs (red with transparency)
        if (!jobData.spikeArrivalTimes.isEmpty()) {
            addSeries(chart, "Spike Jobs (" + jobData.spikeArrivalTimes.size() + ")",
                    jobData.spikeArrivalTimes, jobData.spikeSizes, new Color(200, 50, 50));
        }

        displayAndSaveChart(chart, "size_vs_arrival.svg");
    }

    /**
     * Creates a scatter plot showing response time vs arrival time.
     */
    public void plotResponseTimeVsArrivalTime() throws IOException {
        ResponseTimeData data = readResponseTimeData();

        XYChart chart = createChart("Response Time vs Arrival Time", "Arrival Time", "Response Time");

        // Add series for low current SI (blue with transparency)
        if (!data.lowSiArrivalTimes.isEmpty()) {
            addSeries(chart, "CurrentSi < 10 (" + data.lowSiArrivalTimes.size() + ")",
                    data.lowSiArrivalTimes, data.lowSiResponseTimes, new Color(0, 100, 200));
        }

        // Add series for high current SI (red with transparency)
        if (!data.highSiArrivalTimes.isEmpty()) {
            addSeries(chart, "CurrentSi >= 10 (" + data.highSiArrivalTimes.size() + ")",
                    data.highSiArrivalTimes, data.highSiResponseTimes, new Color(200, 50, 50));
        }

        displayAndSaveChart(chart, "response_time_vs_arrival.svg");
    }

    private JobData readJobData() throws IOException {
        JobData data = new JobData();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            // Skip header
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 6) continue;

                double arrivalTime = Double.parseDouble(values[1].trim());
                double size = Double.parseDouble(values[4].trim());
                boolean isSpike = Boolean.parseBoolean(values[5].trim());

                if (isSpike) {
                    data.spikeArrivalTimes.add(arrivalTime);
                    data.spikeSizes.add(size);
                } else {
                    data.normalArrivalTimes.add(arrivalTime);
                    data.normalSizes.add(size);
                }
            }
        }

        return data;
    }

    private ResponseTimeData readResponseTimeData() throws IOException {
        ResponseTimeData data = new ResponseTimeData();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");

                // Skip header
                if (values[0].equalsIgnoreCase("jobId")) continue;

                if (values.length < 5) continue;

                double arrivalTime = Double.parseDouble(values[1].trim());
                double responseTime = Double.parseDouble(values[3].trim());
                boolean currentSiHigh = Boolean.parseBoolean(values[4].trim());

                if (currentSiHigh) {
                    data.highSiArrivalTimes.add(arrivalTime);
                    data.highSiResponseTimes.add(responseTime);
                } else {
                    data.lowSiArrivalTimes.add(arrivalTime);
                    data.lowSiResponseTimes.add(responseTime);
                }
            }
        }

        return data;
    }

    private XYChart createChart(String title, String xAxisTitle, String yAxisTitle) {
        XYChart chart = new XYChartBuilder()
                .width(CHART_WIDTH)
                .height(CHART_HEIGHT)
                .title(title)
                .xAxisTitle(xAxisTitle)
                .yAxisTitle(yAxisTitle)
                .build();

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setMarkerSize(MARKER_SIZE);

        // Enhanced readability settings
        chart.getStyler().setChartTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        chart.getStyler().setAxisTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        chart.getStyler().setAxisTickLabelsFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        chart.getStyler().setLegendFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));

        // Better grid and colors for readability
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setPlotGridLinesColor(Color.LIGHT_GRAY);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);

        // Better legend positioning
        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideE);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Vertical);

        // Add some padding
        chart.getStyler().setPlotMargin(20);

        return chart;
    }

    private void addSeries(XYChart chart, String name, List<Double> xData, List<Double> yData, Color color) {
        XYSeries series = chart.addSeries(name, xData, yData);
        series.setMarker(SeriesMarkers.CIRCLE);
        series.setMarkerColor(color);

        // Add transparency for overlapping points
        Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
        series.setMarkerColor(transparentColor);

        // Remove fill to make dots more visible
        series.setFillColor(Color.WHITE);
    }

    private void displayChart(XYChart chart) {
        SwingWrapper<XYChart> wrapper = new SwingWrapper<>(chart);
        javax.swing.JFrame frame = wrapper.displayChart();

        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
    }

    private void displayAndSaveChart(XYChart chart, String filename) {
        //displayChart(chart);
        saveChart(chart, filename);
    }

    private void saveChart(XYChart chart, String filename) {
        try {
            Files.createDirectories(Path.of("results"));
            Path outputPath = Path.of("results/", filename);

            if (Files.exists(outputPath)) {
                Files.delete(outputPath);
            }

            if (filename.toLowerCase().endsWith(".svg")) {
                // For SVG export, use the full path including extension
                org.knowm.xchart.VectorGraphicsEncoder.saveVectorGraphic(
                        chart,
                        outputPath.toString(),
                        org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat.SVG
                );
            } else {
                // PNG raster - remove extension as the encoder adds it
                String baseName = outputPath.toString().replaceAll("\\.png$", "");
                org.knowm.xchart.BitmapEncoder.saveBitmap(
                        chart,
                        baseName,
                        org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG
                );
            }
            logger.log(Level.INFO, "Chart saved to: {0}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save chart", e);
        }
    }

    /**
     * Data holder for job information grouped by spike status.
     */
    private static class JobData {
        final List<Double> spikeArrivalTimes = new ArrayList<>();
        final List<Double> spikeSizes = new ArrayList<>();
        final List<Double> normalArrivalTimes = new ArrayList<>();
        final List<Double> normalSizes = new ArrayList<>();
    }

    /**
     * Data holder for response time information grouped by current SI status.
     */
    private static class ResponseTimeData {
        final List<Double> highSiArrivalTimes = new ArrayList<>();
        final List<Double> highSiResponseTimes = new ArrayList<>();
        final List<Double> lowSiArrivalTimes = new ArrayList<>();
        final List<Double> lowSiResponseTimes = new ArrayList<>();
    }
}