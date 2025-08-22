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

public class genArrivals {
    private final Rvgs rvgs;
    private final HyperExponential serviceTimeObj;

    public genArrivals(double serviceCv, double serviceMean) {
        Rngs rngs = new Rngs();
        rngs.plantSeeds(-1);
        System.out.println("Used seeds: "+ Arrays.toString(rngs.getSeedArray()));
        this.rvgs = new Rvgs(rngs);
        this.serviceTimeObj = new HyperExponential(serviceCv, serviceMean);
    }

    public void generateArrivals(int count, String csvPath) {
        int sStreamP = 3;
        int sStreamExp1 = 4;
        int sStreamExp2 = 5;
        try {
            // Crea la directory se non esiste
            Files.createDirectories(Paths.get(csvPath).getParent());
            // Cancella il file se esiste
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
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file CSV: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        double serviceCv = 4;
        double serviceMean = 0.16;
        int numArrivi = 100000;
        String csvPath = "results/genArrivals.csv";

        genArrivals generator = new genArrivals(serviceCv, serviceMean);
        generator.generateArrivals(numArrivi, csvPath);

        PlotCSV plotter = new PlotCSV();
        plotter.plotIdVsSize();
    }
}