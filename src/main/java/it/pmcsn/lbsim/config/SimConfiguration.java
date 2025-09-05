package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingType;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadType;
import it.pmcsn.lbsim.models.simulation.runType.RunType;

import java.time.Duration;

public interface SimConfiguration {
    RunType getRunType();
    long getSeed();
    int getNumberOfBatchs();
    int getBatchSize();
    int getNumberOfReplicas();
    int getDurationInJobs();
    Duration getDurationInSeconds();
    int getMaxLag();


    boolean getIsTracedriven();
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
    int getSpikeCpuMultiplier();
    double getSpikeCpuPercentage();

    boolean isHorizontalEnabled();
    int getSlidingWindowSize();
    Duration getR0max();
    Duration getR0min();
    Duration getHorizontalCoolDown();
    int getInitialServerCount();

    String getCsvOutputDir();
    String getPlotOutputDir();

    String getTraceArrivalsPath();
    String getTraceSizePath();

    WorkloadType getChooseWorkload();


}

