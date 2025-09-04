package it.pmcsn.lbsim.models.simulation.runType;

public enum RunType {
    INFINITESIMULATION,
    FINITESIMULATIONJOBS,
    FINITESIMULATIONTIME,
    AUTOCORRELATION;

    public static RunType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Run type cannot be null");
        }
        return switch (value.toLowerCase()) {
            case "infinitesimulation" -> INFINITESIMULATION;
            case "finitesimulationjobs" -> FINITESIMULATIONJOBS;
            case "finitesimulationtime" -> FINITESIMULATIONTIME;
            case "autocorrelation" -> AUTOCORRELATION;
                default -> throw new IllegalArgumentException("Unknown type of run: " + value);
        };
    }
}
