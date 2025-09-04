package it.pmcsn.lbsim.utils.runType;

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
            case "infinitesimualtion" -> INFINITESIMULATION;
            case "finitesimulationjobs" -> FINITESIMULATIONJOBS;
            case "finitesimualtiontime" -> FINITESIMULATIONTIME;
            case "autocorrelation" -> AUTOCORRELATION;
                default -> throw new IllegalArgumentException("Unknown type of run: " + value);
        };
    }
}
