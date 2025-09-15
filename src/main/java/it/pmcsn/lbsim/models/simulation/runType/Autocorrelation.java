package it.pmcsn.lbsim.models.simulation.runType;

import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.simulation.FutureEventList;
import it.pmcsn.lbsim.models.simulation.JobStats;
import it.pmcsn.lbsim.utils.AutoCorrelationFunction;
import it.pmcsn.lbsim.utils.csv.CsvAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Autocorrelation implements RunPolicy {
    private final AutoCorrelationFunction acs;
    private final CsvAppender autocorrCsv;
    private static final Logger logger = Logger.getLogger(Autocorrelation.class.getName());

    // Warm-up: usa o il tempo o il numero di job (se null, ignorato)
    private final Double  warmupEndTime;   // es. in secondi simulati
    private final Integer warmupJobs;      // es. primi N job da scartare
    private long completedJobs = 0;

    public Autocorrelation(int maxLag) {
        this(maxLag, /*warmupEndTime*/ null, /*warmupJobs*/ 5_000);
    }

    public Autocorrelation(int maxLag, Double warmupEndTime, Integer warmupJobs) {
        this.acs = new AutoCorrelationFunction(maxLag);
        this.warmupEndTime = warmupEndTime;
        this.warmupJobs    = warmupJobs;
        try {
            this.autocorrCsv = new CsvAppender(
                    Path.of("output/csv/autocorrelation.csv"),
                    "lag", "autocorrelation"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateArrivalStats(double time, JobStats newJobStats, LoadBalancer loadBalancer) {
        // no-op
    }

    @Override
    public void updateDepartureStats(double currentTime, JobStats departureJob, LoadBalancer loadBalancer, double responseTime) {
        if (departureJob.getJob().getAssignedServer().getId() == -1) {
            // job scartato (es. load balancer ha rifiutato il job)
            return;
        }
        // Assunto: questo metodo viene chiamato UNA VOLTA PER JOB con il suo responseTime.
        completedJobs++;

        // warm-up: se definito a tempo o per #job
        if ((warmupEndTime != null && currentTime < warmupEndTime)
                || (warmupJobs != null && completedJobs <= warmupJobs)) {
            return;
        }

        // filtra valori non validi
        if (Double.isNaN(responseTime) || Double.isInfinite(responseTime) || responseTime < 0.0) {
            logger.log(Level.FINE, "Scartato responseTime non valido: {0}", responseTime);
            return;
        }

        // aggiungi un punto alla serie
        acs.iteration(responseTime);
    }

    @Override
    public void updateFinalStats() {
        // chiudi calcolo ACF
        acs.finish();
        double[] r = acs.autocorrelation();    // r[0]=1
        long nObs = acs.count();
        int  K    = r.length - 1;

        // salva CSV
        for (int j = 0; j < r.length; j++) {
            autocorrCsv.writeRow(String.valueOf(j), String.valueOf(r[j]));
        }

        // cutoff "da libro": banda ±2/sqrt(n), con finestra di stabilità (es. 10 lag)
        int window = 10;                             // puoi variare (5–20 va bene)
        int L = acs.computeBookCutoff(window);

        // stampa un log chiaro
        double band = (nObs > 0) ? 2.0 / Math.sqrt((double) nObs) : Double.NaN;
        logger.log(Level.INFO,
                String.format("ACF: nObs=%d, K=%d, banda=±%.6f (2/sqrt(n)), cutoff(L)=%d con window=%d",
                        nObs, K, band, L, window));

        // avviso sulla qualità (opzionale ma utile)
        if (nObs < 50L * K) {
            logger.log(Level.WARNING,
                    "Poche osservazioni rispetto al maxLag (nObs={0}, K={1}). Consigliato n >= 50*K (meglio 100*K).",
                    new Object[]{nObs, K});
        }
    }

    @Override
    public void closeCsvs() {
        autocorrCsv.close();
    }
}
