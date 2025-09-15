package it.pmcsn.lbsim.utils;

import java.util.Arrays;

/**
 * ACF streaming con buffer circolare e co-somme (versione fedele ad "Acs").
 * Uso:
 *   - chiama iteration(x) per ogni osservazione
 *   - a fine run chiama finish()
 *   - poi autocorrelation() restituisce r[0..K] con r[0]=1
 */
public class AutoCorrelationFunction {

    private final int K;           // massimo lag
    private final int SIZE;        // K + 1
    private final double[] hold;   // ultimi K+1 valori (buffer circolare)
    private final double[] cosum;  // cosum[j] = somma di x[i]*x[i+j]

    private long nObs = 0;         // numero osservazioni
    private double sum = 0.0;      // somma dei valori
    private int p = 0;             // indice corrente nel buffer circolare
    private long iCount = 0;       // contatore come nel codice del libro
    private boolean filled = false;
    private boolean finished = false;

    private double mean = Double.NaN;
    private double[] rho = null;   // r[0..K], r[0]=1

    private Integer ciCutoffK = null;

    public AutoCorrelationFunction(int maxLag) {
        if (maxLag < 1) throw new IllegalArgumentException("maxLag deve essere >= 1");
        this.K = maxLag;
        this.SIZE = K + 1;
        this.hold = new double[SIZE];
        this.cosum = new double[SIZE];
        Arrays.fill(this.cosum, 0.0);
    }

    /** Inserisci un dato x (es. tempo di risposta di un job). */
    public void iteration(double x) {
        if (finished) throw new IllegalStateException("finish() già chiamato");

        // 1) riempi i primi K+1 valori (come "the first K+1 data values")
        if (!filled) {
            hold[(int) iCount] = x;
            sum += x;
            nObs++;
            iCount++;
            if (iCount == SIZE) {
                filled = true;
                iCount = SIZE; // allineamento col codice del libro
            }
            return;
        }

        // 2) buffer pieno: aggiorna cosum e fai scorrere il buffer
        for (int j = 0; j < SIZE; j++) {
            cosum[j] += hold[p] * hold[(p + j) % SIZE];
        }
        sum += x;
        nObs++;
        hold[p] = x;
        p = (p + 1) % SIZE;
        iCount++;
    }

    /** Chiudi i conti e calcola r[0..K]. Va chiamato una sola volta a fine run. */
    public void finish() {
        if (finished) return;
        finished = true;

        // Caso: non ho riempito il buffer -> stima degenerata
        if (!filled) {
            mean = (nObs > 0) ? (sum / nObs) : Double.NaN;
            rho = new double[SIZE];
            Arrays.fill(rho, 0.0);
            rho[0] = 1.0;
            return;
        }

        // 3) "empty the circular array": aggiungi SIZE passi alle co-somme
        long n = nObs;
        long i = iCount;
        while (i < n + SIZE) {
            for (int j = 0; j < SIZE; j++) {
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            }
            hold[p] = 0.0;
            p = (p + 1) % SIZE;
            i++;
        }

        // 4) media e covarianze: cov[j] = cosum[j]/(n-j) - mean^2
        mean = sum / n;
        for (int j = 0; j <= K; j++) {
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);
        }

        // 5) normalizza: r[j] = cov[j]/cov[0], con r[0]=1
        rho = new double[SIZE];
        if (cosum[0] <= 0.0) {
            Arrays.fill(rho, 0.0);
            rho[0] = 1.0;
        } else {
            rho[0] = 1.0;
            for (int j = 1; j <= K; j++) {
                rho[j] = cosum[j] / cosum[0];
            }
        }
    }

    /** Restituisce r[0..K] (r[0]=1). Chiama finish() se necessario. */
    public double[] autocorrelation() {
        if (!finished) finish();
        return Arrays.copyOf(rho, rho.length);
    }

    // --- accessor opzionali, non cambiano l'algoritmo ---
    public long count() { return nObs; }
    public double mean() { if (!finished) finish(); return mean; }
    public int getMaxLag() { return K; }

    public int computeBookCutoff(int window) {
        if (!finished) finish();
        if (rho == null || rho.length < 2 || nObs <= 0) {
            ciCutoffK = 0;
            return ciCutoffK;
        }

        double band = 2.0 / Math.sqrt((double) nObs);   // ±2/√n
        int consec = 0;
        int L = K;                                      // default: conservativo

        for (int j = 1; j <= K; j++) {
            if (Math.abs(rho[j]) <= band) {
                consec++;
                if (consec >= window) {                 // primo punto di entrata "stabile"
                    L = j - window + 1;
                    break;
                }
            } else {
                consec = 0;
            }
        }
        ciCutoffK = L;
        return L;
    }

    /** Ultimo cutoff calcolato con computeBookCutoff (può essere null se mai chiamato). */
    public Integer getBookCutoff() { return ciCutoffK; }
}
