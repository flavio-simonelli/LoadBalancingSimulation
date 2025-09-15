package it.pmcsn.lbsim.models.domain.schedulingpolicy;

public enum SchedulingType {
    LEAST_LOAD,
    ROUND_ROBIN;

    public static SchedulingType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Scheduling policy cannot be null");
        }
        switch (value.toLowerCase()) {
            case "least_load":
                return LEAST_LOAD;
            case "round_robin":
                return ROUND_ROBIN;
            default:
                throw new IllegalArgumentException("Unknown scheduling policy: " + value);
        }
    }
}
