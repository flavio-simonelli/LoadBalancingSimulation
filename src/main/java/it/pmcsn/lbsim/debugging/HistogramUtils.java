package it.pmcsn.lbsim.debugging;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;

public class HistogramUtils {

    // Regola di Sturges
    private static int sturgesRule(int n) {
        return (int) Math.ceil(1 + (Math.log(n) / Math.log(2))); // log base 2
    }

    // Regola di Wand
    private static int wandRule(int n) {
        return (int) Math.ceil((5.0 / 3.0) * Math.cbrt(n)); // radice cubica
    }

    // Ritorna una scelta "media" consigliata
    private static int chooseK(int n) {
        // esempio: media tra Sturges e radice quadrata
        int k1 = sturgesRule(n);
        int k2 = wandRule(n);
        System.out.println("k1 sturgesRule : "+k1+ " k2 wandRule: "+k2);
        return max(k1, k2);
    }

    // Estremo inferiore: floor(log2(n))
    private static int kMin(int n) {
        return (int) Math.floor(Math.log(n) / Math.log(2));
    }

    // Estremo superiore: floor(sqrt(n))
    private static int kMax(int n) {
        return (int) Math.floor(Math.sqrt(n));
    }

    public static int chooseDefinitiveK (int n){
        int chooseK = chooseK(n);
        int kmin = kMin(n);
        int kmax = kMax(n);
        System.out.println(" k min : "+kmin+ " k max : "+kmax);
        if( kmin<= chooseK && chooseK <= kmax) {
            printStats(chooseK);
            return chooseK;
        }else {
            if (chooseK > kmax) {
                printStats(chooseK);
                return kmax;
            }else{
                printStats(chooseK);
                return kmin;
            }
        }
    }

    private static void printStats(int chooseK) {
        System.out.println("k scelto = " + chooseK);

    }

    public static double binWidth(double a, double b, int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Number of bins must be greater than zero");
        }
        return (b - a) / k;
    }

    public static double relativeFrequency(int binCount, int totalCount) {
        if (totalCount <= 0) {
            throw new IllegalArgumentException("Total count must be greater than zero");
        }
        return (double) binCount / totalCount;
    }

    public static double stimatedDensity(double relativeFrequency, double binWidth) {
        if (binWidth <= 0.0) {
            throw new IllegalArgumentException("Bin width must be greater than zero");
        }
        return relativeFrequency / binWidth;
    }

    public static double  stimatedStdDev(double a, double binWidth, int k,int[] binCounts, int totalCount, double mean) {
        return Math.sqrt(stimatedVariance(a, binWidth, k, binCounts, totalCount, mean));
    }

    public static double binMidpoint(double a, double binWidth, int j) {
        return a + (j + 0.5) * binWidth;
    }

    public static double stimatedMean(double a, double binWidth, int k, int[] binCounts, int totalCount) {
        if (binCounts.length != k) {
            throw new IllegalArgumentException("Bin counts array length must match number of bins");
        }
        if (totalCount <= 0) {
            throw new IllegalArgumentException("Total count must be greater than zero");
        }

        double mean = 0.0;
        for (int i = 0; i < k; i++) {
            double midpoint = binMidpoint(a, binWidth, i);
            double relativeFreq = binCounts[i] / (double) totalCount;
            mean += midpoint * relativeFreq;
        }
        return mean;
    }

    public static double stimatedVariance(double a, double binWidth, int k,
                                          int[] binCounts, int totalCount, double mean) {
        if (binCounts.length != k) {
            throw new IllegalArgumentException("Bin counts array length must match number of bins");
        }
        if (totalCount <= 0) {
            throw new IllegalArgumentException("Total count must be greater than zero");
        }

        double variance = 0.0;
        for (int i = 0; i < k; i++) {
            double midpoint = binMidpoint(a, binWidth, i);
            double relativeFreq = binCounts[i] / (double) totalCount;
            variance += Math.pow(midpoint - mean, 2) * relativeFreq;
        }

        // correzione binning
        variance += (binWidth * binWidth) / 12.0;

        return variance;
    }

    public static double theoreticalVariance(double cv, double mean){
        return (cv * cv) * (mean * mean);
    }

    /**
     * Calcola il percentile P (in [0,1]) dei dati (non ordinati).
     * Usa il metodo "nearest-rank": posizione = ceil(P * N).
     */
    public static double percentile(List<Double> data, double p) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("Percentile p must be between 0 and 1");
        }

        // converto List<Double> in array di double
        double[] sorted = data.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);

        int N = sorted.length;
        if (p == 0.0) {
            return sorted[0];
        }
        if (p == 1.0) {
            return sorted[N - 1];
        }

        int index = (int) Math.ceil(p * N) - 1;
        return sorted[index];
    }

    /**
     * Calcola a e b come quantili alpha e (1 - alpha).
     */
    public static double[] quantileRange(List<Double> data, double alpha) {
        double a = percentile(data, alpha);
        double b = percentile(data, 1.0 - alpha);
        return new double[] { a, b };
    }

}
