package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;

public class FullExpWorkloadGenerator implements WorkloadGenerator{
    private final Rngs rngs;
    private final Rvgs rvgs;
    private final Double meanArrival;
    private final int streamArrival;
    private final Double meanservice;
    private final int streamService;

    public FullExpWorkloadGenerator(Rngs rngs, Double meanArrival, int StreamArrival, Double meanService, int StreamService) {
        this.rvgs = new Rvgs(rngs);
        this.rngs = rngs;
        this.meanArrival = meanArrival;
        this.streamArrival = StreamArrival;
        this.meanservice = meanService;
        this.streamService = StreamService;
    }

    @Override
    public double nextArrival(double currentTime) {
        rngs.selectStream(streamArrival);
        return rvgs.exponential(this.meanArrival)+currentTime;
    }

    @Override
    public double nextJobSize() {
        rngs.selectStream(streamService);
        return rvgs.exponential(this.meanservice);
    }
}