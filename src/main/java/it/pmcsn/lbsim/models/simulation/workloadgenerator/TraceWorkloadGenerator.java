package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Workload generator che legge arrivi e job size da due file.
 * - Il file arrivi contiene una lista di timestamp (uno per riga).
 * - Il file size contiene una lista di dimensioni (uno per riga).
 * Vincolo: sizes.size() >= arrivals.size()
 */
public class TraceWorkloadGenerator implements WorkloadGenerator {

    private final List<Double> arrivals;
    private final List<Double> sizes;
    private final Iterator<Double> arrivalIt;
    private final Iterator<Double> sizeIt;

    private double lastArrival = Double.NaN;
    private double lastSize = Double.NaN;

    public TraceWorkloadGenerator(Path arrivalsFile, Path sizesFile) throws IOException {
        this.arrivals = readDoubles(arrivalsFile);
        this.sizes = readDoubles(sizesFile);

        if (sizes.size() < arrivals.size()) {
            throw new IllegalArgumentException(
                    "Trace file error: number of sizes (" + sizes.size() +
                            ") is smaller than number of arrivals (" + arrivals.size() + ")");
        }

        this.arrivalIt = arrivals.iterator();
        this.sizeIt = sizes.iterator();
    }

    @Override
    public double nextArrival(double currentTime) {
        if (!arrivalIt.hasNext()) {
            return Double.POSITIVE_INFINITY; // end of trace
        }
        lastArrival = arrivalIt.next();
        if (!sizeIt.hasNext()) {
            throw new IllegalStateException("Ran out of job sizes before arrivals were finished");
        }
        lastSize = sizeIt.next();
        return lastArrival;
    }

    @Override
    public double nextJobSize() {
        if (Double.isNaN(lastSize)) {
            throw new IllegalStateException("nextArrival() must be called before nextJobSize()");
        }
        return lastSize;
    }

    private static List<Double> readDoubles(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Double::parseDouble)
                .toList();
    }

    public int getSizeArrival() {
        return arrivals.size();
    }
}
