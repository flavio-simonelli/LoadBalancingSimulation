package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;

public class VerifyWorkloadGenerator implements WorkloadGenerator{
    private final Rngs rngs;
    private final Rvgs rvgs;
    private final Double meanArrival;
    private final int streamArrival;
    private final HyperExponential service;

    public VerifyWorkloadGenerator(Rngs rngs, Double meanArrival, int StreamArrival, HyperExponential service) {
        this.rvgs = new Rvgs(rngs);
        this.rngs = rngs;
        this.meanArrival = meanArrival;
        this.streamArrival = StreamArrival;
        this.service = service;
    }

    @Override
    public double nextArrival(double currentTime) {
        rngs.selectStream(streamArrival);
        return rvgs.exponential(this.meanArrival)+currentTime;
    }

    @Override
    public double nextJobSize() {
        return rvgs.hyperExponential(
                service.getP(), service.getM1(), service.getM2(),
                service.getStreamP(), service.getStreamExp1(), service.getStreamExp2());
    }
}
