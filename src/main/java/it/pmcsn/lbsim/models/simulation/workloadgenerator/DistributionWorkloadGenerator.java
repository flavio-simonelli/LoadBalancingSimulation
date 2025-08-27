package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rvgs;

public class DistributionWorkloadGenerator implements WorkloadGenerator {
    private final Rvgs rvgs;
    private final HyperExponential interarrival;
    private final HyperExponential service;

    public DistributionWorkloadGenerator(Rvgs rvgs, HyperExponential interarrival, HyperExponential service) {
        this.rvgs = rvgs;
        this.interarrival = interarrival;
        this.service = service;
    }

    @Override
    public double nextArrival(double currentTime) {
        double delta = rvgs.hyperExponential(
                interarrival.getP(), interarrival.getM1(), interarrival.getM2(),
                interarrival.getStreamP(), interarrival.getStreamExp1(), interarrival.getStreamExp2());
        return currentTime + delta;
    }

    @Override
    public double nextJobSize() {
        return rvgs.hyperExponential(
                service.getP(), service.getM1(), service.getM2(),
                service.getStreamP(), service.getStreamExp1(), service.getStreamExp2());
    }
}
