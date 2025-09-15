package it.pmcsn.lbsim.models.simulation.workloadgenerator;

public interface WorkloadGenerator {
    /**
     * Restituisce il tempo del prossimo arrivo a partire da `currentTime`.
     */
    double nextArrival(double currentTime);

    /**
     * Restituisce la dimensione del job (tempo di servizio richiesto).
     */
    double nextJobSize();
}

