package it.pmcsn.lbsim.config;

import java.time.Duration;

public class Config {

    public Simulation simulation;
    public InfiniteSimulation infinitesimulation;
    public FiniteSimulation finitesimulation;
    public Autocorrelation autocorrelation;
    public Workload workload;
    public Scheduling scheduling;
    public Scaling scaling;
    public Output output;
    public Logging logging;
    public Path path;

    public static class Simulation {
        public String typesimulation;
        public long seed;

    }

    public static class InfiniteSimulation {
        public int k;
        public int b;
    }

    public static class FiniteSimulation {
        public int replica;
        public int numjobs;
        public Duration duration;
    }

    public static class Autocorrelation {
        public int maxlag;
    }

    public static class Workload {
        public String chooseWorkload;
        public boolean isTracedriven;
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
        public int cpuMultiplier;
        public double cpuPercentage;
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

    public static class Path {
        public String traceArrivalsPath;
        public String traceSizePath;
    }
}
