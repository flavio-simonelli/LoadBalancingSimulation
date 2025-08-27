package it.pmcsn.lbsim.config;

import java.time.Duration;

public class Config {

    public Simulation simulation;
    public Workload workload;
    public Scheduling scheduling;
    public Scaling scaling;
    public Output output;
    public Logging logging;

    public static class Simulation {
        public boolean isFirstSimulation;
        public long seed0;
        public long seed1;
        public long seed2;
        public long seed3;
        public long seed4;
        public long seed5;
        public Duration duration;
    }

    public static class Workload {
        public Dist interarrival;
        public Dist service;
    }

    public static class Dist {
        public double mean;
        public double cv;
        public int streamp;
        public int streamhexp1;
        public int streamhexp2;
    }

    public static class Scheduling {
        public String policy;
    }

    public static class Scaling {
        public SpikeServer spikeServer;
        public Horizontal horizontal;
    }

    public static class SpikeServer {
        public boolean enabled;
        public int SImax;
        public int SImin;
        public int cpuMultiplier;
        public double cpuPercentage;
        public Duration coolDown;
    }

    public static class Horizontal {
        public boolean enabled;
        public int slidingWindowSize;
        public Duration R0max;
        public Duration R0min;
        public Duration coolDown;
        public int initialServerCount;
    }

    public static class Output {
        public String csvDir;
        public String plotDir;
    }

    public static class Logging {
        public String level; // es: "INFO", "FINE", "WARNING"
    }
}
