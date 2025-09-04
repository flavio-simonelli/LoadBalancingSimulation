package it.pmcsn.lbsim.utils.runType.finiteHorizon;

import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.runType.RunPolicy;

import java.io.IOException;
import java.nio.file.Path;

public class FiniteHorizon implements RunPolicy {
    private final CsvAppender welfordCsv;


    public FiniteHorizon(long seed, int i, int j) {

        try {
            welfordCsv = new CsvAppender(Path.of("output/csv/Welford" + seed + "rep" + i + "j" + j + ".csv"), "Type", "N", "Mean", "StdDev", "Variance", "Semi Intervallo media");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void updateResponseTimeStats(double reponseTime) {

    }

    @Override
    public CsvAppender getCsv() {
        return welfordCsv;
    }
}
