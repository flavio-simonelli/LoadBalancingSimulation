package it.pmcsn.lbsim.utils;


import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Implementazione OO dell'algoritmo ACS del libro:
 * - mantiene un buffer circolare di SIZE = K+1 osservazioni
 * - accumula cosum[j] = somma di x[i] * x[i+j]
 * - normalizza a fine raccolta per ottenere ACF (autocorrelazione) ai lag 1..K
 *
 * NOTE:
 *  - Per coerenza con il codice del libro, la logica è identica.
 *  - Si assume di fornire almeno K+1 osservazioni (come l'esempio originale).
 *  - Dopo finish() l'istanza è “chiusa” (non chiamare più add()).
 */
public class AutoCorrelationFunction {

    private final int K;          // massimo lag
    private final int SIZE;       // K + 1 (dimensione del buffer)
    private final double[] hold;  // buffer circolare con gli ultimi K+1 valori
    private final double[] cosum; // cosum[j] accumula x[i] * x[i+j]

    // stato di accumulo
    private int i = 0;            // quanti punti ho ricevuto (indice "dati letti")
    private int p = 0;            // testa del buffer circolare
    private double sum = 0.0;     // somma dei valori per la media
    private boolean finished = false;

    // risultati finali
    private long n = 0;           // numero totale di dati (prima del draining)
    private double mean = Double.NaN;
    private double[] acf = null;  // ACF[0..K] con acf[0]=1.0

    public AutoCorrelationFunction(int K) {
        if (K < 1) throw new IllegalArgumentException("K deve essere >= 1");
        this.K = K;
        this.SIZE = K + 1;
        this.hold  = new double[SIZE];
        this.cosum = new double[SIZE];
        // cosum[] parte a zero per definizione
    }

    /**
     * Aggiunge un'osservazione x.
     * La logica ricalca l'originale:
     *  - per i primi SIZE valori: riempie solo hold[] e somma
     *  - dal (SIZE+1)-esimo in poi: aggiorna cosum con i prodotti dei valori nel buffer,
     *    poi inserisce x nella posizione p e fa avanzare il puntatore circolare.
     */
    public void add(double x) {
        ensureNotFinished();

        if (i < SIZE) {
            // fase di "priming": solo riempimento iniziale del buffer
            sum     += x;
            hold[i]  = x;
            i++;
            return;
        }

        // fase principale: aggiorna cosum con l'elemento più vecchio e i successivi
        for (int j = 0; j < SIZE; j++) {
            cosum[j] += hold[p] * hold[(p + j) % SIZE];
        }

        // inserisci il nuovo valore, avanza il puntatore, aumenta il conteggio e la somma
        hold[p] = x;
        p = (p + 1) % SIZE;
        sum += x;
        i++;
    }

    /**
     * Chiude la raccolta, esegue il "draining" del buffer (identico all'originale),
     * normalizza e calcola l'ACF. Dopo questa chiamata l'oggetto è chiuso.
     */
    public void finish() {
        ensureNotFinished();
        if (i < SIZE) {
            throw new IllegalStateException(
                    "Per coerenza con l'algoritmo ACS originale servono almeno K+1 osservazioni."
            );
        }

        // n = numero punti “veri” raccolti prima del draining
        n = i;

        // draining del buffer come nel codice del libro
        while (i < n + SIZE) {
            for (int j = 0; j < SIZE; j++) {
                cosum[j] += hold[p] * hold[(p + j) % SIZE];
            }
            hold[p] = 0.0;
            p = (p + 1) % SIZE;
            i++;
        }

        // media e trasformazione cosum in covarianze campionarie
        mean = sum / n;
        for (int j = 0; j <= K; j++) {
            cosum[j] = (cosum[j] / (n - j)) - (mean * mean);
        }

        // ACF: r[j] = cov(j) / var, con var = cosum[0]
        acf = new double[K + 1];
        acf[0] = 1.0;
        double var = cosum[0];
        for (int j = 1; j <= K; j++) {
            acf[j] = (var == 0.0) ? 0.0 : (cosum[j] / var);
        }

        finished = true;
    }

    /**
     * Salva su CSV i punti (lag, acf) per j=1..K (come stampa il main originale).
     */
    public void saveAcfToCsv(Path file) {
        requireFinished();
        try (CsvAppender csv = new CsvAppender(file, "Lag", "ACF")) {
            for (int j = 1; j <= K; j++) {
                csv.writeRow(String.valueOf(j), String.valueOf(acf[j]));
            }
        } catch (IOException e) {
            throw new RuntimeException("Errore scrivendo CSV ACF", e);
        }
    }

    // ---------------------------
    // Getter risultati (post-finish)
    // ---------------------------

    public long getCount()          { requireFinished(); return n; }
    public double getMean()         { requireFinished(); return mean; }
    /** dev. std. popolazionale (sqrt(var)), come nel main originale (sqrt(cosum[0])) */
    public double getStdDev()       { requireFinished(); return Math.sqrt(cosum[0]); }
    /** array ACF[0..K], con ACF[0]=1.0 */
    public double[] getAcf()        { requireFinished(); return acf.clone(); }
    /** covarianza(j) già normalizzata come nel codice originale */
    public double getCovarianceAtLag(int j) {
        requireFinished();
        if (j < 0 || j > K) throw new IllegalArgumentException("Lag fuori range");
        return cosum[j];
    }

    // ---------------------------
    // Utility
    // ---------------------------

    private void ensureNotFinished() {
        if (finished) {
            throw new IllegalStateException("AcsAutocorrelation già chiusa: non è possibile aggiungere altri dati.");
        }
    }

    private void requireFinished() {
        if (!finished) {
            throw new IllegalStateException("Chiama finish() prima di leggere i risultati.");
        }
    }
}
