package it.pmcsn.lbsim.models.domain.scaling.horizontalscaler;

import java.util.logging.Logger;

/**
 * Policy: mantiene una finestra mobile degli ultimi N response time.
 * - Se media > R0max  ⇒ SCALE_OUT (se rispettato cooldown).
 * - Se media < R0min  ⇒ SCALE_IN  (se rispettato cooldown).
 * - Altrimenti NONE.
 *
 * Note:
 * - Usa isteresi (R0min < R0max) per evitare flip/flop.
 * - Il cooldown limita la frequenza delle azioni.
 */
public class SlidingWindowHorizontalScaler implements HorizontalScaler {

    private static final Logger logger = Logger.getLogger(SlidingWindowHorizontalScaler.class.getName());

    private final SlidingWindowResponseTime window;
    private final double r0min;
    private final double r0max;
    private final double cooldownSec;

    private double lastActionAt = Double.NEGATIVE_INFINITY;

    /**
     * @param windowSize   numero di campioni nella sliding window (>=1)
     * @param r0min        soglia inferiore per scale-in (>=0)
     * @param r0max        soglia superiore per scale-out (r0max > r0min)
     * @param cooldownSec  secondi minimi tra due azioni di scaling (>=0)
     */
    public SlidingWindowHorizontalScaler(int windowSize, double r0min, double r0max, double cooldownSec) {
        if (windowSize < 1) throw new IllegalArgumentException("windowSize must be >= 1");
        if (r0min < 0 || r0max <= r0min) throw new IllegalArgumentException("Require 0 <= r0min < r0max");
        if (cooldownSec < 0) throw new IllegalArgumentException("cooldownSec must be >= 0");

        this.window = new SlidingWindowResponseTime(windowSize);
        this.r0min = r0min;
        this.r0max = r0max;
        this.cooldownSec = cooldownSec;
    }

    @Override
    public Action notifyJobDeparture(double rt, double now) {
        if (rt < 0) throw new IllegalArgumentException("response time must be >= 0");
        window.add(rt);

        // Se la finestra non è ancora “piena”, possiamo comunque agire, ma è comune attendere qualche campione.
        double mean = window.getAverage();

        // Rispetta il cooldown tra azioni
        if ((now - lastActionAt) < cooldownSec) {
            return Action.NONE;
        }

        if (mean > r0max) {
            return Action.SCALE_OUT;
        }
        if (mean < r0min) {
            return Action.SCALE_IN;
        }
        return Action.NONE;
    }

    @Override
    public void setLastActionAt(double time) {
        this.lastActionAt = time;
    }
}
