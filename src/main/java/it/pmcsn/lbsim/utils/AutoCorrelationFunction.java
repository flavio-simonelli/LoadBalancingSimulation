package it.pmcsn.lbsim.utils;

import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;

public class AutoCorrelationFunction {
    private final int K;             // lag massimo
    private final int SIZE;          // buffer size = K+1
    private final double[] hold;     // buffer circolare
    private final double[] cosum;    // accumulo prodotti x[i]*x[i+j]
    private double sum;              // somma valori
    private long count;              // numero osservazioni
    private int p;                   // indice testa buffer
    private final CsvAppender csv;

    public AutoCorrelationFunction(int K, String fileName) {
        this.K = K;
        this.SIZE = K + 1;
        this.hold = new double[SIZE];
        this.cosum = new double[SIZE];
        this.sum = 0.0;
        this.count = 0;
        this.p = 0;

        try {
            this.csv = new CsvAppender(
                    Path.of("output/csv/" + fileName),
                    "Lag", "ACF"
            );
        } catch (IOException e) {
            throw new RuntimeException("Errore creando CSV ACF", e);
        }
    }

    public void add(double x) {
        // aggiorna cosum con i valori correnti del buffer
        for (int j = 0; j < SIZE; j++) {
            cosum[j] += hold[(p + j) % SIZE] * x;
        }

        // aggiorna buffer circolare
        hold[p] = x;
        p = (p + 1) % SIZE;

        // aggiorna contatori globali
        sum += x;
        count++;
    }

    /**
     * Calcola l'array ACF [0..K]
     */
    public double[] getACF() {
        double[] acf = new double[K + 1];
        if (count < 2) return acf;

        double mean = sum / count;

        // ricrea copia cosum normalizzata
        for (int j = 0; j <= K; j++) {
            double cov = (cosum[j] / (count - j)) - (mean * mean);
            if (j == 0) {
                acf[0] = 1.0; // sempre 1
            } else {
                acf[j] = cov / ((cosum[0] / count) - (mean * mean));
            }
        }
        return acf;
    }

    /**
     * Trova il cutoff lag: primo lag con |acf[j]| < threshold
     * per almeno consecutive passi.
     */
    public int findCutoffLag(double threshold, int consecutive) {
        double[] acf = getACF();
        int consec = 0;
        for (int j = 1; j < acf.length; j++) {
            if (Math.abs(acf[j]) < threshold) {
                consec++;
                if (consec >= consecutive) {
                    return j - consecutive + 1;
                }
            } else {
                consec = 0;
            }
        }

        return acf.length - 1; // fallback
    }

    public void saveToCsv() {
        double[] acf = getACF();
        try {
            for (int j = 1; j <= K; j++) {
                csv.writeRow(String.valueOf(j), String.valueOf(acf[j]));
            }
            csv.close();
        } catch (Exception e) {
            throw new RuntimeException("Errore scrivendo ACF", e);
        }
    }
}
