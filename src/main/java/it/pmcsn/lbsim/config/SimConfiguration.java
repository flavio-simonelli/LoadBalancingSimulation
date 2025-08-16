package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

public interface SimConfiguration {
    double getDuration();
    int getSImax();
    int getSImin();
    double getR0max();
    double getR0min();
    int getSlidingWindowSize();
    int getInitialServerCount();
    int getCpuMultiplierSpike();
    double getCpuPercentageSpike();
    SchedulingType getSchedulingType();
}
