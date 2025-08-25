package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.time.Duration;

public interface SimConfiguration {
    Duration getDurationSeconds();
    double getInterarrivalMean();
    double getInterarrivalCv();
    double getServiceMean();
    double getServiceCv();

    SchedulingType getSchedulingType();

    boolean isSpikeEnabled();
    int getSImax();
    int getSImin();
    int getSpikeCpuMultiplier();
    double getSpikeCpuPercentage();
    Duration getSpikeCoolDown();

    boolean isHorizontalEnabled();
    int getSlidingWindowSize();
    Duration getR0max();
    Duration getR0min();
    Duration getHorizontalCoolDown();
    int getInitialServerCount();

    String getCsvOutputDir();
    String getPlotOutputDir();
}

