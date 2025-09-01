package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.random.Rvgs;

/**
 * A truly exponential M/M/1 workload generator for testing theoretical values
 */
public class TrueExponentialWorkloadGenerator implements WorkloadGenerator {
    private final Rvgs rvgs;
    private final double meanArrival;
    private final double meanService;

    public TrueExponentialWorkloadGenerator(Rvgs rvgs, double meanArrival, double meanService) {
        this.rvgs = rvgs;
        this.meanArrival = meanArrival;
        this.meanService = meanService;
    }

    @Override
    public double nextArrival(double currentTime) {
        return rvgs.exponential(this.meanArrival) + currentTime;
    }

    @Override
    public double nextJobSize() {
        // Return truly exponential service times
        return rvgs.exponential(this.meanService);
    }
}