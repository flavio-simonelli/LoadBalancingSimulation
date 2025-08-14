package it.pmcsn.lbsim.config;

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
}
