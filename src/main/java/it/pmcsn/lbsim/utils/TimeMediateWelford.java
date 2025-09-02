package it.pmcsn.lbsim.utils;

public class TimeMediateWelford {

    private int i=0;
    private double avg =0.0;
    private double variance =0.0;
    private double time;
    private double lastX;

    public TimeMediateWelford(double initialTime, double initialX) {
        if (initialTime < 0.0) {
            throw new IllegalArgumentException("Initial time must be non-negative");
        }
        this.time = initialTime;
        this.lastX = initialX;
    }

    public TimeMediateWelford(double initialTime) {
        this(initialTime,0.0);
    }

    public TimeMediateWelford() {
        this(0.0,0.0);
    }


    public void iteration(double x, double newTime) {
        i++;
        double d = this.lastX - avg;
        this.lastX = x;

        double delta = newTime - this.time;

        this.variance = this.variance + ((d * d) * ((delta * this.time) / newTime));
        this.avg = this.avg + d* delta/newTime;

        this.time = newTime;


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

}
