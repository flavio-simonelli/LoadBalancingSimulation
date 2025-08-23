package it.pmcsn.lbsim.utils.debugging;

import it.pmcsn.lbsim.utils.plot.PlotCSV;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;
import it.pmcsn.lbsim.utils.random.HyperExponential;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class DebuggerGenerator {
    private final Rvgs rvgs;
    private double mean;
    private final Rngs rngs;

    public DebuggerGenerator() {
        this.rngs = new Rngs();
        rngs.plantSeeds(-1);
        System.out.println("Used seeds: " + Arrays.toString(rngs.getSeedArray()));
        this.rvgs = new Rvgs(rngs);
    }

    public void generateExponentialArrivals(int count, double meanExp, String csvPath) {
        this.mean = 0;
        int sStreamExp = 3;
        try {
            Files.createDirectories(Paths.get(csvPath).getParent());
            Files.deleteIfExists(Paths.get(csvPath));
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvPath))) {
                writer.println("id,size");
                for (int i = 0; i < count; i++) {
                    rngs.selectStream(sStreamExp);
                    double size = rvgs.exponential(meanExp);
                    writer.println(i + "," + size);
                    this.mean += size;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file CSV: " + e.getMessage());
        }
    }

    public void generateHyperExponentialArrivals(int count, double serviceCv, double serviceMean, String csvPath) {
        mean = 0;
        int sStreamP = 3;
        int sStreamExp1 = 4;
        int sStreamExp2 = 5;
        HyperExponential serviceTimeObj = new HyperExponential(serviceCv, serviceMean);
        try {
            Files.createDirectories(Paths.get(csvPath).getParent());
            Files.deleteIfExists(Paths.get(csvPath));
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvPath))) {
                writer.println("id,size");
                for (int i = 0; i < count; i++) {
                    double size = rvgs.hyperExponential(
                            serviceTimeObj.getP(),
                            serviceTimeObj.getM1(),
                            serviceTimeObj.getM2(),
                            sStreamP, sStreamExp1, sStreamExp2
                    );
                    writer.println(i + "," + size);
                    mean += size;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        int numArrivi = 10000;
        String csvPathExp = "results/genArrivals.csv";
        //String csvPathHyp = "results/genHyperExponentialArrivals.csv";

        DebuggerGenerator generator = new DebuggerGenerator();

        // Esempio generazione esponenziale
        double meanExp = 5;
        generator.generateExponentialArrivals(numArrivi, meanExp, csvPathExp);
        System.out.println("Evaluated mean is: " + generator.mean / numArrivi);

//        // Esempio generazione iperesponenziale
//        double serviceCv = 1.0000001;
//        double serviceMean = 10.0;
//        generator.generateHyperExponentialArrivals(numArrivi, serviceCv, serviceMean, csvPathHyp);
//        System.out.println("Mean hyperexponential: " + mean / numArrivi);

        // Eventuale plotting
        PlotCSV plotter = new PlotCSV();
        plotter.plotIdVsSize();
    }
}