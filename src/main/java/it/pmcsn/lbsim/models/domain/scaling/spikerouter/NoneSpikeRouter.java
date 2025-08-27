package it.pmcsn.lbsim.models.domain.scaling.spikerouter;

import it.pmcsn.lbsim.models.domain.server.Server;

public class NoneSpikeRouter implements SpikeRouter{
    @Override
    public Action decide(Server chosen, double nowSeconds) {
        return Action.ASSIGN_TO_CHOSEN;
    }
}
