package it.pmcsn.lbsim.models.domain.scaling.horizontalscaler;

/**
 * Decide azioni di scaling orizzontale in base ai campioni di response time.
 * Tipico ciclo: ad ogni departure → onResponseSample(rt, now) → Action.
 */
public interface HorizontalScaler {

    enum Action { NONE, SCALE_OUT, SCALE_IN }

    /**
     * Notifica un nuovo campione di response time al tempo 'now' (tempo simulato)
     * e ritorna l'azione da intraprendere (se il cooldown lo consente).
     */
    Action notifyJobDeparture(double responseTimeSeconds, double nowSeconds);

}
