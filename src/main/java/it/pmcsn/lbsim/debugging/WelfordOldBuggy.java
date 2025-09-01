package it.pmcsn.lbsim.debugging;

/**
 * Create a modified WelfordSimple with the old buggy implementation for comparison
 */
public class WelfordOldBuggy {
    private int i=0;
    private double avg =0.0;
    private double variance =0.0;

    public void iteration(double x) {
        i++;
        double d = x - avg;
        this.variance = this.variance + d * d * (i - 1.0) / i;  // OLD BUGGY FORMULA
        this.avg = this.avg + d / i;
    }

    public int getI() {
        return this.i;
    }

    public double getAvg() {
        return this.avg;
    }

    public double getVariance() {
        return this.variance / (i);  // OLD: Population variance
    }

    public double getStandardVariation() {
        return Math.sqrt(this.variance /(i));  // OLD: Population std dev
    }
}