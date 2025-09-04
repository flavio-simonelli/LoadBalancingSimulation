package it.pmcsn.lbsim.utils;

public class WelfordSimple {

    private int i=0;
    private double avg =0.0;
    private double variance =0.0;


    public void iteration(double x) {
        i++;
        double d = x - avg;
        this.variance = this.variance + d * d * (i - 1.0) / i;
        this.avg = this.avg + d / i;
    }

    public int getI() {
        return this.i;
    }

    public double getAvg() {
        return this.avg;
    }

    public double getVariance() {
        return this.variance / (i);
    }

    public double getStandardVariation() {
        return Math.sqrt(this.variance /(i));
    }


    public void reset() {
        this.i = 0;
        this.avg = 0.0;
        this.variance = 0.0;
    }

}
