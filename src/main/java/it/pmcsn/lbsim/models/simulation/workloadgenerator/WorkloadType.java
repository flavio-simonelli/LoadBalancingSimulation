package it.pmcsn.lbsim.models.simulation.workloadgenerator;

public enum WorkloadType {
    HYPEREXPONENTIAL,
    EXPONENTIAL,
    TRACE,
    FULLEXP;

    public static WorkloadType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Scheduling policy cannot be null");
        }
        return switch (value.toLowerCase()) {
            case "hyperexponential" -> HYPEREXPONENTIAL;
            case "exponential" -> EXPONENTIAL;
            case "trace" -> TRACE;
            case "fullexp" -> FULLEXP;
            default -> throw new IllegalArgumentException("Unknown scheduling policy: " + value);
        };
    }
}