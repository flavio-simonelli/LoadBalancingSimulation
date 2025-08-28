package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.plot.PlotCSV;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rvms;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistributionGenerator {

    private static final String CONFIG_FILE_PATH = "config.yaml";
    private static final int EXPONENTIAL_STREAM = 0;
    private static final int HYPEREXP_P_STREAM = 1;
    private static final int HYPEREXP_STREAM_1 = 2;
    private static final int HYPEREXP_STREAM_2 = 3;

    private static final Logger logger = Logger.getLogger(DistributionGenerator.class.getName());

    private final Rngs rngs;
    private final Rvgs rvgs;

    public DistributionGenerator() {
        this.rngs = new Rngs();
        this.rngs.plantSeeds(-1);
        this.rvgs = new Rvgs(rngs);
        logger.log(Level.INFO,"Generated seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));
    }

    public double generateExponentials(int sampleCount, double meanValue, String outputPath) {
        validateInputs(sampleCount, meanValue, outputPath);

        double cumulativeSum = 0.0;
        Path path = Paths.get(outputPath);

        try (CsvAppender csvAppender = new CsvAppender(path, "id", "value")) {
            for (int i = 0; i < sampleCount; i++) {
                rngs.selectStream(EXPONENTIAL_STREAM);
                double sample = rvgs.exponential(meanValue);
                csvAppender.writeRow(String.valueOf(i), String.valueOf(sample));
                cumulativeSum += sample;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV file: " + e.getMessage(), e);
        }

        return cumulativeSum / sampleCount;
    }

    public double generateHyperExponentials(int sampleCount, double coefficientOfVariation,
                                            double meanValue, String outputPath) {
        validateInputs(sampleCount, meanValue, outputPath);
        validateCoefficientOfVariation(coefficientOfVariation);

        double cumulativeSum = 0.0;
        HyperExponential hyperExponentialParams = new HyperExponential(coefficientOfVariation, meanValue, 1, 2, 3);
        Path path = Paths.get(outputPath);

        try (CsvAppender csvAppender = new CsvAppender(path, "id", "value")) {
            for (int i = 0; i < sampleCount; i++) {
                double sample = rvgs.hyperExponential(
                        hyperExponentialParams.getP(),
                        hyperExponentialParams.getM1(),
                        hyperExponentialParams.getM2(),
                        HYPEREXP_P_STREAM,
                        HYPEREXP_STREAM_1,
                        HYPEREXP_STREAM_2
                );
                csvAppender.writeRow(String.valueOf(i), String.valueOf(sample));
                cumulativeSum += sample;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV file: " + e.getMessage(), e);
        }

        return cumulativeSum / sampleCount;
    }

    private void validateInputs(int sampleCount, double meanValue, String outputPath) {
        if (sampleCount <= 0) {
            throw new IllegalArgumentException("Sample count must be positive");
        }
        if (meanValue <= 0) {
            throw new IllegalArgumentException("Mean value must be positive");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
    }

    private void validateCoefficientOfVariation(double cv) {
        if (cv <= 0) {
            throw new IllegalArgumentException("Coefficient of variation must be positive");
        }
    }

    public static void main(String[] args) throws Exception {
        SimConfiguration config = ConfigLoader.load(CONFIG_FILE_PATH);
        double mean = 0.15;
        double cv = 4.0;
        String pdfOutputPath = config.getCsvOutputDir() + "HyperExponentialPDF.csv";
        Path pdfPath = Paths.get(pdfOutputPath);

        double totalArea = 0.0;
        double stepSize = 0.001;

        try (CsvAppender csvAppender = new CsvAppender(pdfPath, "value", "probability")) {
            for (int i = 0; i <= 1000; i++) {
                double value = i / 1000.0;
                double probability = Rvms.pdfHyperexponential(mean, cv, value);
                csvAppender.writeRow(String.valueOf(value), String.valueOf(probability));

                totalArea += probability * stepSize;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write PDF CSV file: " + e.getMessage(), e);
        }

        System.out.println("Area totale sotto la curva: " + totalArea);
        PlotCSV.plotScatter(String.valueOf(pdfPath), config.getPlotOutputDir(), "value", "probability");

        int numberOfSamples = 1000;
        //String exponentialOutputPath = config.getCsvOutputDir() + "ExponentialGen.csv";
        String hyperExponentialOutputPath = config.getCsvOutputDir() + "HyperExponentialGen.csv";

        DistributionGenerator generator = new DistributionGenerator();

        // Exponential distribution
        double exponentialMean = 5.0;
//        double exponentialSampleMean = generator.generateExponentials(
//                numberOfSamples, exponentialMean, exponentialOutputPath);
//        double exponentialTheoreticalStd = exponentialMean; // Per esponenziale: std = mean
//        SampleStats exponentialStats = calculateStatistics(exponentialOutputPath, exponentialSampleMean);

//        logger.log(Level.INFO,
//                "Exponential - Sample mean: {0} (theoretical: {1}), Sample std: {2} (theoretical: {3}), Min: {4}, Max: {5}\n",
//                new Object[]{exponentialSampleMean, exponentialMean, exponentialStats.standardDeviation, exponentialTheoreticalStd, exponentialStats.minValue, exponentialStats.maxValue});

        // Hyperexponential distribution
        double hyperExponentialCv = 4.0;
        double hyperExponentialMean = 0.15;
        double hyperExponentialSampleMean = generator.generateHyperExponentials(
                numberOfSamples, hyperExponentialCv, hyperExponentialMean, hyperExponentialOutputPath);
        double hyperExponentialTheoreticalStd = hyperExponentialCv * hyperExponentialMean; // std = cv * mean
        SampleStats hyperExponentialStats = calculateStatistics(hyperExponentialOutputPath, hyperExponentialSampleMean);

        logger.log(Level.INFO,
                "Hyperexponential - Sample mean: {0} (theoretical: {1}), Sample std: {2} (theoretical: {3}), Min: {4}, Max: {5}\n",
                new Object[]{hyperExponentialSampleMean, hyperExponentialMean, hyperExponentialStats.standardDeviation, hyperExponentialTheoreticalStd, hyperExponentialStats.minValue, hyperExponentialStats.maxValue});

        //PlotCSV.plotScatter(exponentialOutputPath, config.getPlotOutputDir(), "id", "value");
        PlotCSV.plotScatter(hyperExponentialOutputPath, config.getPlotOutputDir(), "id", "value");
    }

    private static class SampleStats {
        double standardDeviation;
        double minValue;
        double maxValue;

        SampleStats(double standardDeviation, double minValue, double maxValue) {
            this.standardDeviation = standardDeviation;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
    }

    private static SampleStats calculateStatistics(String csvPath, double sampleMean) throws IOException {
        double sumSquaredDifferences = 0.0;
        int count = 0;
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(Paths.get(csvPath))) {
            reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                double value = Double.parseDouble(parts[1]);
                sumSquaredDifferences += Math.pow(value - sampleMean, 2);
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
                count++;
            }
        }

        double standardDeviation = Math.sqrt(sumSquaredDifferences / (count - 1));
        return new SampleStats(standardDeviation, minValue, maxValue);
    }
}