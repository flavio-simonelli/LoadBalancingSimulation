package it.pmcsn.lbsim.models.domain.scaling.spikerouter;

import it.pmcsn.lbsim.models.domain.server.Server;

public interface SpikeRouter {

    enum Action { ASSIGN_TO_CHOSEN, ROUTE_TO_SPIKE }

    /**
     * Decide se un job deve restare sul server scelto o andare allo spike.
     *
     * @param chosen   server selezionato dalla scheduling policy
     * @return l’azione da intraprendere
     */
    Action decide(Server chosen, double nowSeconds);
}
