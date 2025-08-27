package it.pmcsn.lbsim.utils.random;


public class HyperExponential {
    private final double p;
    private final int streamP;
    private final double m1;
    private final int streamExp1;
    private final double m2;
    private final int streamExp2;

    public HyperExponential(double cv, double m, int streamP, int streamExp1, int streamExp2) {
        if (m <= 0.0) {
            throw new IllegalArgumentException("Mean must be > 0");
        }
        if (cv <= 1.0) {
            throw new IllegalArgumentException("CV must be > 1 for hyperexponential");
        }
        this.p= calculateP(cv);
        this.m1 = calculateMHyper(m, this.p);
        this.m2 = calculateMHyper(m, (1 - this.p));
        this.streamP = streamP;
        this.streamExp1 = streamExp1;
        this.streamExp2 = streamExp2;
    }

    public HyperExponential(double p, double m1, double m2, int streamP, int streamExp1, int streamExp2) {
        this.p = p;
        this.m1 = m1;
        this.m2 = m2;
        this.streamP = streamP;
        this.streamExp1 = streamExp1;
        this.streamExp2 = streamExp2;
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
        return 1/(2*p*mu);
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

    public int getStreamP() {
        return streamP;
    }

    public int getStreamExp1() {
        return streamExp1;
    }

    public int getStreamExp2() {
        return streamExp2;
    }
}
