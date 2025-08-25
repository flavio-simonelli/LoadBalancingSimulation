package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.plot.PlotCSV;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.utils.random.HyperExponential;

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
        HyperExponential hyperExponentialParams = new HyperExponential(coefficientOfVariation, meanValue);
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
        int numberOfSamples = 10000;
        String exponentialOutputPath = config.getCsvOutputDir() + "ExponentialGen.csv";
        String hyperExponentialOutputPath = config.getCsvOutputDir() + "HyperExponentialGen.csv";

        DistributionGenerator generator = new DistributionGenerator();

        double exponentialMean = 5.0;
        double exponentialSampleMean = generator.generateExponentials(
                numberOfSamples, exponentialMean, exponentialOutputPath);
        logger.log(Level.INFO,
                "Exponential sample mean: {0} (theoretical: {1})\n",
                new Object[]{exponentialSampleMean, exponentialMean});


        double hyperExponentialCv = 4.0;
        double hyperExponentialMean = 0.15;
        double hyperExponentialSampleMean = generator.generateHyperExponentials(
            numberOfSamples, hyperExponentialCv, hyperExponentialMean, hyperExponentialOutputPath);
        logger.log(Level.INFO, "Hyperexponential sample mean: {0} (theoretical: {1})\n",
                new Object[]{hyperExponentialSampleMean, hyperExponentialMean});


        PlotCSV.plotScatter(exponentialOutputPath, config.getPlotOutputDir(), "id", "value");
        PlotCSV.plotScatter(hyperExponentialOutputPath, config.getPlotOutputDir(), "id", "value");
    }
}
