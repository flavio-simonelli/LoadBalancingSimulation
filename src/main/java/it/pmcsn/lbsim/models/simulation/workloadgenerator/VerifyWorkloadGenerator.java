package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rvgs;

public class VerifyWorkloadGenerator implements WorkloadGenerator{
    private final Rvgs rvgs;
    private final Double meanArrival;
    private final HyperExponential service;

    public VerifyWorkloadGenerator(Rvgs rvgs, Double meanArrival, HyperExponential service) {
        this.rvgs = rvgs;
        this.meanArrival = meanArrival;
        this.service = service;
    }

    @Override
    public double nextArrival(double currentTime) {
        return rvgs.exponential(this.meanArrival)+currentTime;
    }

    @Override
    public double nextJobSize() {
        return rvgs.hyperExponential(
                service.getP(), service.getM1(), service.getM2(),
                service.getStreamP(), service.getStreamExp1(), service.getStreamExp2());
    }
}
