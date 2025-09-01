package it.pmcsn.lbsim.utils;

public class WelfordSimple {

    private int i=0;
    private double avg =0.0;
    private double variance =0.0;


    public void iteration(double x) {
        i++;
        double delta = x - avg;
        this.avg = this.avg + delta / i;
        double delta2 = x - this.avg;
        this.variance = this.variance + delta * delta2;
    }

    public int getI() {
        return this.i;
    }

    public double getAvg() {
        return this.avg;
    }

    public double getVariance() {
        return i > 1 ? this.variance / (i - 1) : 0.0;  // Sample variance
    }

    public double getStandardVariation() {
        return i > 1 ? Math.sqrt(this.variance / (i - 1)) : 0.0;  // Sample standard deviation
    }

}
