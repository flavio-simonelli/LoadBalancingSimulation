package it.pmcsn.lbsim.models.domain.scaling.spikerouter;

import it.pmcsn.lbsim.models.domain.server.Server;

import java.util.logging.Logger;

public class SimpleSpikeRouter implements SpikeRouter {

    private static final Logger log = Logger.getLogger(SimpleSpikeRouter.class.getName());

    private final int siMax;

    public SimpleSpikeRouter(int siMax) {
        if (siMax <= 0) throw new IllegalArgumentException("SImax must be > 0");
        this.siMax = siMax;
    }

    @Override
    public Action decide(Server chosen, double nowSeconds) {
        if (chosen.getCurrentSI() >= siMax) {
            return Action.ROUTE_TO_SPIKE;
        }
        return Action.ASSIGN_TO_CHOSEN;
    }
}
