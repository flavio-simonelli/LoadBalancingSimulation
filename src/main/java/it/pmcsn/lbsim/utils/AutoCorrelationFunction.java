
package it.pmcsn.lbsim.utils;

public class AutoCorrelationFunction {
    private final int lagMax;
    private final double[] buffer;
    private final double[] cosum;
    private int head;
    private int count;
    private final WelfordSimple welford;

    public AutoCorrelationFunction(int lagMax) {
        this.lagMax = lagMax;
        this.buffer = new double[lagMax + 1];
        this.cosum = new double[lagMax + 1];
        this.head = -1;
        this.count = 0;
        this.welford = new WelfordSimple();
    }

    public void add(double x) {
        count++;
        welford.iteration(x);

        // aggiorna buffer circolare con valore grezzo
        head = (head + 1) % (lagMax + 1);
        buffer[head] = x;

        // aggiorna cosum (con valori grezzi, centratura avverrà dopo)
        int idx = head;
        for (int j = 0; j <= lagMax; j++) {
            cosum[j] += buffer[head] * buffer[idx];
            idx = (idx + 1) % (lagMax + 1);
        }
    }

    public double[] getACF() {
        double[] acf = new double[lagMax + 1];
        if (count < 2) return acf;

        double mean = welford.getAvg();
        double c0 = welford.getVariance() * count; // totale M2

        for (int j = 0; j <= lagMax; j++) {
            // normalizza cosum con correzione della media
            double adj = cosum[j] - (count - j) * mean * mean;
            acf[j] = adj / c0;
        }
        return acf;
    }

    public int getCount() { return count; }
    public double getMean() { return welford.getAvg(); }
    public double getVariance() { return welford.getVariance(); }
    public double getStdDev() { return welford.getStandardVariation(); }

    public static void main(String[] args) {
        AutoCorrelationFunction acf = new AutoCorrelationFunction(20);

        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < 10000; i++) {
            double x = -Math.log(1 - rnd.nextDouble()); // Exp(1)
            acf.add(x);
        }

        double[] r = acf.getACF();
        for (int j = 0; j < r.length; j++) {
            System.out.printf("lag %d: %.4f%n", j, r[j]);
        }
    }
}
