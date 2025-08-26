package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;

import java.time.Duration;

public interface SimConfiguration {
    boolean isFirstSimulation();
    long getSeed0();
    long getSeed1();
    long getSeed2();
    long getSeed3();
    long getSeed4();
    long getSeed5();
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

