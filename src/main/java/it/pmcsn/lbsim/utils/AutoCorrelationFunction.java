package it.pmcsn.lbsim.utils;

import java.util.Arrays;

/**
 * Calcolo "one-pass" dell'autocorrelazione fino al lag K usando un buffer circolare di ampiezza K+1.
 * Logica identica a Acs.java (Park & Geyer), ma con API streaming:
 * - chiama iteration(x) per ogni nuovo dato
 * - chiama finish() quando non arrivano più dati (effettua lo "svuotamento" del buffer)
 * - poi leggi mean(), stdev(), autocovarianceAtLag(j), autocorrelation()
 *
 * NOTE IMPORTANTI:
 * - Servono almeno K+1 punti prima di chiamare finish(), come nel programma originale.
 * - Le stime di autocovarianza usano denominatore (n - j) e la varianza è "biased", identica all'originale.
 */
public class AutoCorrelationFunction {

    private final int K;           // lag massimo
    private final int SIZE;        // K + 1
    private final double[] hold;   // buffer circolare degli ultimi K+1 valori
    private final double[] cosum;  // accumuli di sum x_i * x_{i+j}
    private int p = 0;             // indice testa del buffer (elemento più "vecchio")
    private int filled = 0;        // quanti elementi del buffer sono stati riempiti (<= SIZE)
    private long n = 0;            // conteggio totale punti
    private double sum = 0.0;      // somma dei valori (per la media)
    private boolean finished = false;

    /**
     * @param K lag massimo (>=1)
     */
    public AutoCorrelationFunction(int K) {
        if (K < 1) {
            throw new IllegalArgumentException("K deve essere >= 1");
        }
        this.K = K;
        this.SIZE = K + 1;
        this.hold = new double[SIZE];
        this.cosum = new double[SIZE];
        // cosum[] inizializzato a 0.0 di default
    }

    /**
     * Invia un nuovo dato al calcolatore.
     * Mantiene la stessa sequenza operativa dell'originale:
     * - finché non abbiamo SIZE (=K+1) dati, riempiamo il buffer senza aggiornare cosum[]
     * - quando il buffer è pieno: prima accumuliamo i prodotti con l'elemento più vecchio hold[p],
     * poi sovrascriviamo hold[p] con il nuovo x e avanziamo p
     */
    public void iteration(double x) {
        if (finished) {
            throw new IllegalStateException("finish() già chiamato: crea un nuovo calcolatore o chiama reset()");
        }

        if (filled < SIZE) {
            // fase di bootstrap: riempi il buffer
            hold[filled++] = x;
            sum += x;
            n++;
            return; // identico allo schema originale (nessun aggiornamento cosum finché non è pieno)
        }

        // buffer pieno: accumula i prodotti con l'elemento più vecchio e poi inserisci x
        for (int j = 0; j < SIZE; j++) {
            cosum[j] += hold[p] * hold[(p + j) % SIZE];
        }
        sum += x;
        hold[p] = x;
        p = (p + 1) % SIZE;
        n++;
    }

    /**
     * Completa il calcolo effettuando lo "svuotamento" del buffer (exactly like the original):
     * per SIZE volte:
     * - accumula cosum[j] con hold[p] * hold[(p + j) % SIZE]
     * - poi azzera hold[p] e avanza p
     */
    public void finish() {
        if (finished) return;

        if (n < SIZE) {
            throw new IllegalStateException("Servono almeno K+1 = " + SIZE + " dati prima di finish().");
        }

        int i = 0;
        while (i < SIZE) {
            for (int j = 0; j < SIZE; j++) {
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            }
            hold[p] = 0.0;
            p = (p + 1) % SIZE;
            i++;
        }
        finished = true;
    }

    /**
     * Numero totale di dati visti.
     */
    public long count() {
        return n;
    }

    /**
     * Media dei dati.
     */
    public double mean() {
        if (n == 0) return Double.NaN;
        return sum / n;
    }

    /**
     * Deviazione standard "biased" (sqrt(gamma_0)). Richiede finish().
     */
    public double stdev() {
        checkFinished();
        return Math.sqrt(autocovarianceAtLag(0));
    }

    /**
     * Autocovarianza "biased" al lag j: gamma_j = cosum[j]/(n - j) - mean^2.
     * Richiede finish(). Valida per 0 <= j <= K (con n >= K+1 garantito).
     */
    public double autocovarianceAtLag(int j) {
        checkFinished();
        if (j < 0 || j > K) {
            throw new IllegalArgumentException("j deve essere in [0, " + K + "]");
        }
        double m = mean();
        return (cosum[j] / (n - j)) - (m * m);
    }

    /**
     * Autocorrelazioni r[0..K], con r[0] = 1.
     * Richiede finish().
     */
    public double[] autocorrelation() {
        checkFinished();
        double m = mean();
        double gamma0 = (cosum[0] / (n - 0)) - (m * m);
        double[] r = new double[SIZE];
        r[0] = 1.0;
        for (int j = 1; j <= K; j++) {
            double gammaj = (cosum[j] / (n - j)) - (m * m);
            r[j] = gammaj / gamma0;
        }
        return r;
    }

    /**
     * Reset completo dello stato per riutilizzare l’istanza.
     */
    public void reset() {
        Arrays.fill(hold, 0.0);
        Arrays.fill(cosum, 0.0);
        p = 0;
        filled = 0;
        n = 0;
        sum = 0.0;
        finished = false;
    }

    private void checkFinished() {
        if (!finished) {
            throw new IllegalStateException("Chiama finish() prima di leggere i risultati.");
        }
    }

    /**
     * Suggerisce un cutoff K' usando bande di significatività ±1.96/√n.
     * Default: window=8 lag consecutivi dentro le bande, margin=4 di sicurezza.
     * Restituisce un valore clampato a min(K, floor(n/10)), e ≥ 1.
     * Richiede finish().
     */
    public int suggestCutoff() {
        return suggestCutoff(8, 4);
    }

    /**
     * Versione parametrica.
     * @param window quanti lag consecutivi devono restare entro ±1.96/√n (>=1)
     * @param margin margine addizionale dopo il primo lag “stabile” (>=0)
     * @return cutoff K' (1..min(K, floor(n/10))) calcolato data-driven;
     *         se non si trova stabilità, usa fallback di Newey–West.
     */
    public int suggestCutoff(int window, int margin) {
        checkFinished();
        if (window < 1) throw new IllegalArgumentException("window deve essere >= 1");
        if (margin < 0) margin = 0;

        // Upper bound di sicurezza
        int maxByN = (int) Math.max(1, Math.floor(n / 10.0));
        int hardMax = Math.min(K, maxByN);

        double[] r = autocorrelation();     // r[0..K], r[0]=1
        double thr = 1.96 / Math.sqrt(n);   // bande di significatività

        int found = -1;
        if (K >= window) {
            outer:
            for (int j = 1; j <= K - window + 1; j++) {
                for (int t = j; t < j + window; t++) {
                    if (Math.abs(r[t]) > thr) {
                        continue outer;
                    }
                }
                found = j; // primo lag da cui inizia una finestra "quieta"
                break;
            }
        }

        int cutoff;
        if (found != -1) {
            long candidate = (long) found + margin;
            if (candidate > Integer.MAX_VALUE) candidate = Integer.MAX_VALUE;
            cutoff = (int) candidate;
        } else {
            // Fallback: banda Newey–West
            double nw = 4.0 * Math.pow(n / 100.0, 2.0 / 9.0);
            cutoff = (int) Math.floor(nw);
            if (cutoff < 1) {
                // Secondo fallback se n è piccolo
                cutoff = (int) Math.max(1, Math.floor(Math.pow(n, 1.0 / 3.0)));
            }
        }

        // Clamp finale
        if (cutoff > hardMax) cutoff = hardMax;
        if (cutoff < 1) cutoff = 1;
        return cutoff;
    }

}