package it.pmcsn.lbsim.utils;

import it.pmcsn.lbsim.utils.random.Rvms;

public class IntervalEstimation {
    private final double LOC; // Level Of Confidence
    private final Rvms rvms;

    public IntervalEstimation(double LOC) {
        if (LOC <= 0.0 || LOC >= 1.0) {
            throw new IllegalArgumentException("LOC must be in (0,1)");
        }
        this.LOC = LOC;
        this.rvms = new Rvms();
    }

    public double SemiIntervalEstimation(double standardDeviation, int n) {
        double u = 1.0 - 0.5 * (1.0 - LOC);
        double t = rvms.idfStudent(n-1, u);
        return t * standardDeviation / Math.sqrt(n-1);
    }

}
