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

    /**
     * Genera la PDF della distribuzione hyper-esponenziale per un range di valori configurabile
     * @param mean Media della distribuzione
     * @param cv Coefficiente di variazione
     * @param a Valore iniziale del range
     * @param b Valore finale del range
     * @param gamma Passo tra i valori
     * @param outputPath Percorso del file CSV di output
     * @return Area totale sotto la curva (approssimazione dell'integrale)
     */
    public double generateHyperExponentialPDF(double mean, double cv, double a, double b,
                                              double gamma, String outputPath) {
        validatePDFInputs(mean, cv, a, b, gamma, outputPath);

        double totalArea = 0.0;
        Path path = Paths.get(outputPath);

        try (CsvAppender csvAppender = new CsvAppender(path, "value", "probability")) {
            for (double value = a; value <= b; value += gamma) {
                double probability = Rvms.pdfHyperexponential(mean, cv, value);
                csvAppender.writeRow(String.valueOf(value), String.valueOf(probability));
                totalArea += probability * gamma;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write PDF CSV file: " + e.getMessage(), e);
        }

        return totalArea;
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

    private void validatePDFInputs(double mean, double cv, double a, double b,
                                   double gamma, String outputPath) {
        if (mean <= 0) {
            throw new IllegalArgumentException("Mean must be positive");
        }
        if (cv <= 0) {
            throw new IllegalArgumentException("Coefficient of variation must be positive");
        }
        if (a < 0) {
            throw new IllegalArgumentException("Start value 'a' must be non-negative");
        }
        if (b <= a) {
            throw new IllegalArgumentException("End value 'b' must be greater than start value 'a'");
        }
        if (gamma <= 0) {
            throw new IllegalArgumentException("Step size 'gamma' must be positive");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
    }

    public static void main(String[] args) throws Exception {
        SimConfiguration config = ConfigLoader.load(CONFIG_FILE_PATH);
        double mean = 0.15;
        double cv = 4.0;
        String pdfOutputPath = config.getCsvOutputDir() + "HyperExponentialPDF.csv";

        // Parametri configurabili per la PDF
        double a = 0.0;      // Valore iniziale
        double b = 2.0;      // Valore finale
        double gamma = 0.01; // Passo

        DistributionGenerator generator = new DistributionGenerator();

        // Genera la PDF con parametri configurabili
        double totalArea = generator.generateHyperExponentialPDF(mean, cv, a, b, gamma, pdfOutputPath);

        System.out.println("Area totale sotto la curva: " + totalArea);
        System.out.println("Range PDF: da " + a + " a " + b + " con passo " + gamma);
        System.out.println("Numero di punti generati: " + (int)((b - a) / gamma + 1));

        PlotCSV.plotScatter(pdfOutputPath, config.getPlotOutputDir(), "value", "probability");

        int numberOfSamples = 1000;
        String hyperExponentialOutputPath = config.getCsvOutputDir() + "HyperExponentialGen.csv";

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