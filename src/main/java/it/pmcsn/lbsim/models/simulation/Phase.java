package it.pmcsn.lbsim.models.simulation;

public class Phase {

    private final double startTime;        // in secondi dall'inizio simulazione
    private final double endTime;          // in secondi dall'inizio simulazione
    private final double meanInterarrival; // mean interarrival time (1/lambda)

    public Phase(double startTime, double endTime, double meanInterarrival) {
        if (endTime <= startTime) {
            throw new IllegalArgumentException("endTime deve essere maggiore di startTime");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.meanInterarrival = meanInterarrival;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double getMeanInterarrival() {
        return meanInterarrival;
    }

    public boolean isInPhase(double time) {
        return time >= startTime && time < endTime;
    }

    @Override
    public String toString() {
        return String.format("Phase[start=%.2f, end=%.2f, meanInterarrival=%.4f]",
                startTime, endTime, meanInterarrival);
    }
}
