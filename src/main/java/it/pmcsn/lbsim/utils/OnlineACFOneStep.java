package it.pmcsn.lbsim.utils;

public class OnlineACFOneStep {
    private final int lagMax;
    private final double[] buffer;   // buffer circolare di lunghezza lagMax+1
    private final double[] cosum;    // accumula somme x_i * x_{i+j}
    private int head;                // indice della testa del buffer
    private int count;               // numero totale di osservazioni viste

    // statistiche globali
    private double mean;
    private double m2;

    public OnlineACFOneStep(int lagMax) {
        this.lagMax = lagMax;
        this.buffer = new double[lagMax + 1];
        this.cosum = new double[lagMax + 1];
        this.head = -1;
        this.count = 0;
        this.mean = 0.0;
        this.m2 = 0.0;
    }

    /**
     * Aggiorna la struttura con un nuovo dato x.
     */
    public void add(double x) {
        count++;

        // aggiorna media/varianza globale con Welford
        double delta = x - mean;
        mean += delta / count;
        m2 += delta * (x - mean);

        // aggiorna buffer circolare
        head = (head + 1) % (lagMax + 1);
        buffer[head] = x;

        // aggiorna cosum: prodotti con tutti i lag possibili
        int idx = head;
        for (int j = 0; j <= lagMax; j++) {
            cosum[j] += buffer[head] * buffer[idx];
            idx = (idx + 1) % (lagMax + 1);
        }
    }

    /**
     * Restituisce l'array delle autocorrelazioni r[0..lagMax].
     */
    public double[] getACF() {
        double[] acf = new double[lagMax + 1];
        if (count < 2) return acf;

        double variance = m2 / count;
        double c0 = variance * count; // somma (x_i - mean)^2

        for (int j = 0; j <= lagMax; j++) {
            // correzione: togli il contributo della media
            double adj = cosum[j] - (count - j) * mean * mean;
            acf[j] = adj / c0;
        }
        return acf;
    }
}
