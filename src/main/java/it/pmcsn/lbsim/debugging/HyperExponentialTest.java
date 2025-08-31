package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.utils.WelfordSimple;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.utils.random.Rvms;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperExponentialTest {
    private static final String configFilePath = "config.yaml"; // Default configuration file path
    private static final String outputResult = "output/csv/hyperexponential_test.csv";
    private static final Logger logger = Logger.getLogger(HyperExponentialTest.class.getName());




    public static void main(String[] args) {
        // read the configuration from a YAML file
        int n = 10000000; // numero di campioni
        SimConfiguration config = ConfigLoader.load(configFilePath);

        double theoreticalMean = config.getInterarrivalMean();
        double theoreticalCv = config.getInterarrivalCv();
        double thoereticalVar = HistogramUtils.theoreticalVariance(theoreticalCv, theoreticalMean);

        // create the hyperexponential test instance
        HyperExponential hyperExponential = new HyperExponential(theoreticalCv, theoreticalMean, config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2());

        CsvAppender hyperexponentialTest;
        try {
            hyperexponentialTest = new CsvAppender(Path.of(outputResult), "type", "mean", "cv", "var");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        hyperexponentialTest.writeRow("theoretical", String.valueOf(theoreticalMean), String.valueOf(theoreticalCv), String.valueOf(thoereticalVar));

        // create a random generator
        Rngs rngs = new Rngs();
        rngs.plantSeeds(config.getSeed0()); // get the first seed from the configuration file
        Rvgs rvgs = new Rvgs(rngs);

        // create Welford for mean and variance
        WelfordSimple welford = new WelfordSimple();

        logger.log(Level.INFO,"Generated seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

        List<Double> samples = new ArrayList<>(n);

        // Generazione campione
        for (int i = 0; i < n; i++) {
            // Scelgo la fase in base alle probabilità
            double hyperSample = rvgs.hyperExponential(hyperExponential.getP(), hyperExponential.getM1(), hyperExponential.getM2(), hyperExponential.getStreamP(), hyperExponential.getStreamExp1(), hyperExponential.getStreamExp2());
            welford.iteration(hyperSample);
            samples.add(hyperSample);
        }

        // Scelta di range [a,b] per l'istogramma
        double alpha = 0.00; // 5%

        double[] range = HistogramUtils.quantileRange(samples, alpha);

        double a = range[0];
        double b = range[1];

        // Media campionaria
        double sampleMean = samples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calcolo del valore k
        int k = HistogramUtils.chooseDefinitiveK(n);

        int[] count = new int[k];
        Double[] freq = new Double[k];
        double gamma = HistogramUtils.binWidth(a, b, k);

        for (Double sample : samples){
            if (sample >= a && sample < b){
                int binIndex = (int) Math.floor((sample - a) / gamma);
                count[binIndex]++;
            }
        }

        // Frequenze relative
        for (int j=0; j<k; j++){
            freq[j]= HistogramUtils.relativeFrequency(count[j], n) ;
        }

        // Densità stimata
        double[] stimatedDenisty = new double[k];
        for (int j=0; j<k; j++){
            stimatedDenisty[j] = HistogramUtils.stimatedDensity(freq[j], gamma);
        }

        //media stimata
        double stimatedMean = HistogramUtils.stimatedMean(a, gamma, k, count, n);
        // Deviazione standard stimata
        double stimatedStdDev = HistogramUtils.stimatedStdDev(a, gamma, k, count, n, stimatedMean);
        // Varianza stimata
        double stimatedVar = HistogramUtils.stimatedVariance(a, gamma, k, count, n, stimatedMean);
        
        // write csv
        hyperexponentialTest.writeRow("histogram", String.valueOf(stimatedMean), String.valueOf(stimatedStdDev/theoreticalMean), String.valueOf(stimatedVar));
        hyperexponentialTest.writeRow("welford", String.valueOf(welford.getAvg()), String.valueOf(welford.getStandardVariation()/welford.getAvg()), String.valueOf(welford.getVariance()));
        hyperexponentialTest.close();


        // Risultati
        System.out.println("Numero di campioni: " + n);
        System.out.println("Media teorica     : " + theoreticalMean);
        System.out.println("Media campionaria : " + sampleMean);
        System.out.println("Media stimated     : " + stimatedMean);
        System.out.println("Varianza teorica  : " + thoereticalVar);
        System.out.println("Varianza campionaria : " + samples.stream().mapToDouble(v -> Math.pow(v - sampleMean, 2)).sum() / (n - 1));
        System.out.println("Varianza stimata : " + stimatedVar);
        System.out.println("Deviazione standard teorica : " + Math.sqrt(thoereticalVar));
        System.out.println("Deviazione standard campionaria : " + Math.sqrt(samples.stream().mapToDouble(v -> Math.pow(v - sampleMean, 2)).sum() / (n - 1)));
        System.out.println("Deviazione standard stimata : " + stimatedStdDev);
        System.out.println("Numero di bin k  : " + k);
        System.out.println("Intervallo [a,b] : [" + a + "," + b + "]");
        System.out.println("Larghezza bin γ  : " + gamma);

    }





}


