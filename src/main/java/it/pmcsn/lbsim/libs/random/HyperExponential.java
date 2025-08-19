package it.pmcsn.lbsim.libs.random;

import it.pmcsn.lbsim.models.LoadBalancer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperExponential {
    private static final Logger logger = Logger.getLogger(HyperExponential.class.getName());
    private double p;
    private double m1;
    private double m2;

    public HyperExponential(double cv, double m) {
        if (m <= 0.0) {
            throw new IllegalArgumentException("Mean must be > 0");
        }
        if (cv <= 1.0) {
            throw new IllegalArgumentException("CV must be > 1 for hyperexponential");
        }
        this.p= calculateP(cv);
        this.m1 = calculateMHyper(m, this.p);
        this.m2 = calculateMHyper(m, (1 - this.p));
    }

    public HyperExponential(double p, double m1, double m2) {
        this.p = p;
        this.m1 = m1;
        this.m2 = m2;
    }

    private double calculateP(double cv) {
        double c2 = cv * cv; // C^2
        double insideSqrt = 1 - (2.0 / (c2 + 1.0));

        if (insideSqrt < 0) {
            throw new IllegalArgumentException("CV troppo piccolo: non esiste soluzione reale.");
        }
        double sqrtTerm = Math.sqrt(insideSqrt);
        double p1 = (1 + sqrtTerm) / 2.0;
        double p2 = (1 - sqrtTerm) / 2.0;
        return Math.min(p1, p2);
    }

    private double calculateMHyper(double m, double p){
        double mu = 1/m;
        return 2*p*mu;
    }

    public double getM1() {
        return m1;
    }

    public double getM2() {
        return m2;
    }

    public double getP() {
        return p;
    }
}
