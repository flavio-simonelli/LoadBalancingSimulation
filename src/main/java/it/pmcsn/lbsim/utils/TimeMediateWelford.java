package it.pmcsn.lbsim.utils;

public class TimeMediateWelford {

    private double mean = 0.0;
    private double m2 = 0.0;        // somma dei quadrati per la varianza
    private double weightSum = 0.0; // tempo totale osservato
    private double lastX;
    private double lastTime;

    public TimeMediateWelford(double initialTime, double initialX) {
        if (initialTime < 0.0) {
            throw new IllegalArgumentException("Initial time must be non-negative");
        }
        this.lastTime = initialTime;
        this.lastX = initialX;
    }

    public TimeMediateWelford(double initialTime) {
        this(initialTime, 0.0);
    }

    public TimeMediateWelford() {
        this(0.0, 0.0);
    }

    public void iteration(double x, double newTime) {
        double dt = newTime - lastTime;
        if (dt < 0.0) {
            throw new IllegalArgumentException("newTime must be >= lastTime");
        }

        // peso = durata intervallo
        double w = dt;
        double delta = lastX - mean;
        weightSum += w;
        mean += (w / weightSum) * delta;
        m2 += w * delta * (lastX - mean);

        lastX = x;
        lastTime = newTime;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        return weightSum > 0 ? m2 / weightSum : 0.0;
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public double getTotalTime() {
        return weightSum;
    }

    public void reset(double initialTime, double initialX) {
        this.mean = 0.0;
        this.m2 = 0.0;
        this.weightSum = 0.0;
        this.lastTime = initialTime;
        this.lastX = initialX;
    }

    public void reset() {
        reset(0.0, 0.0);
    }
}
