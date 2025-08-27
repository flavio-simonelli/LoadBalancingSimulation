package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingType;

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
    int getInterarrivalStreamP();
    int getInterarrivalStreamHexp1();
    int getInterarrivalStreamHexp2();
    double getServiceMean();
    double getServiceCv();
    int getServiceStreamP();
    int getServiceStreamHexp1();
    int getServiceStreamHexp2();


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

