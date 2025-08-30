package it.pmcsn.lbsim.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingType;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadType;

import java.io.InputStream;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {

    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());

    public static SimConfiguration load(String filePath) {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (input == null) {
                throw new IllegalArgumentException("File YAML non trovato: " + filePath);
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.registerModule(new JavaTimeModule());

            Config raw = mapper.readValue(input, Config.class);

            // Configura logging dal file
            if (raw.logging != null && raw.logging.level != null) {
                java.util.logging.Level level = java.util.logging.Level.parse(raw.logging.level.toUpperCase());
                logger.setLevel(level);
                for (var h : Logger.getLogger("").getHandlers()) {
                    h.setLevel(level);
                }
                logger.info("Logger level impostato a: " + level);
            }

            SimConfiguration cfg = new ConfigAdapter(raw);

            // stampa di debug (sarà mostrata solo se livello lo permette)
            printDebug(cfg);

            return cfg;

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento configurazione", e);
        }
    }


    // --- Adapter che traduce Config -> SimConfiguration ---
    private static class ConfigAdapter implements SimConfiguration {
        private final Config cfg;

        ConfigAdapter(Config cfg) {
            this.cfg = cfg;
        }

        @Override public int getReplications() { return cfg.simulation.replications; }
        @Override public long getSeed0() { return cfg.simulation.seed0; }
        @Override public long getSeed1() { return cfg.simulation.seed1; }
        @Override public long getSeed2() { return cfg.simulation.seed2; }
        @Override public long getSeed3() { return cfg.simulation.seed3; }
        @Override public long getSeed4() { return cfg.simulation.seed4; }
        @Override public long getSeed5() { return cfg.simulation.seed5; }
        @Override public boolean getIsTracedriven() {return cfg.workload.isTracedriven;}
        @Override public Duration getDurationSeconds() { return cfg.simulation.duration; }
        @Override public double getInterarrivalMean() { return cfg.workload.interarrival.mean; }
        @Override public double getInterarrivalCv() { return cfg.workload.interarrival.cv; }
        @Override public int getInterarrivalStreamP() { return cfg.workload.interarrival.streamp; }
        @Override public int getInterarrivalStreamHexp1() { return cfg.workload.interarrival.streamhexp1; }
        @Override public int getInterarrivalStreamHexp2() { return cfg.workload.interarrival.streamhexp2; }
        @Override public double getServiceMean() { return cfg.workload.service.mean; }
        @Override public double getServiceCv() { return cfg.workload.service.cv; }
        @Override public int getServiceStreamP() { return cfg.workload.service.streamp; }
        @Override public int getServiceStreamHexp1() { return cfg.workload.service.streamhexp1; }
        @Override public int getServiceStreamHexp2() { return cfg.workload.service.streamhexp2; }

        @Override public SchedulingType getSchedulingType() {
            return SchedulingType.fromString(cfg.scheduling.policy);
        }

        @Override public boolean isSpikeEnabled() { return cfg.scaling.spikeServer.enabled; }
        @Override public int getSImax() { return cfg.scaling.spikeServer.SImax; }
        @Override public int getSImin() { return cfg.scaling.spikeServer.SImin; }
        @Override public int getSpikeCpuMultiplier() { return cfg.scaling.spikeServer.cpuMultiplier; }
        @Override public double getSpikeCpuPercentage() { return cfg.scaling.spikeServer.cpuPercentage; }
        @Override public Duration getSpikeCoolDown() { return cfg.scaling.spikeServer.coolDown; }

        @Override public boolean isHorizontalEnabled() { return cfg.scaling.horizontal.enabled; }
        @Override public int getSlidingWindowSize() { return cfg.scaling.horizontal.slidingWindowSize; }
        @Override public Duration getR0max() { return cfg.scaling.horizontal.R0max; }
        @Override public Duration getR0min() { return cfg.scaling.horizontal.R0min; }
        @Override public Duration getHorizontalCoolDown() { return cfg.scaling.horizontal.coolDown; }
        @Override public int getInitialServerCount() { return cfg.scaling.horizontal.initialServerCount; }

        @Override public String getCsvOutputDir() { return cfg.output.csvDir; }
        @Override public String getPlotOutputDir() { return cfg.output.plotDir; }

        @Override public String getTraceArrivalsPath() { return cfg.path.traceArrivalsPath; }
        @Override public String getTraceSizePath() { return cfg.path.traceSizePath; }

        @Override public WorkloadType getChooseWorkload() { return WorkloadType.fromString(cfg.workload.chooseWorkload); }
    }

    private static void printDebug(SimConfiguration cfg) {
        if (logger.isLoggable(Level.CONFIG)) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== Simulation Config ===\n")
                    .append("Seed0: ").append(cfg.getSeed0()).append("\n")
                    .append("Seed1: ").append(cfg.getSeed1()).append("\n")
                    .append("Seed2: ").append(cfg.getSeed2()).append("\n")
                    .append("Seed3: ").append(cfg.getSeed3()).append("\n")
                    .append("Seed4: ").append(cfg.getSeed4()).append("\n")
                    .append("Seed5: ").append(cfg.getSeed5()).append("\n")
                    .append("Duration (s): ").append(cfg.getDurationSeconds()).append("\n")

                    .append("\n--- Workload ---\n")
                    .append("Interarrival mean: ").append(cfg.getInterarrivalMean()).append("\n")
                    .append("Interarrival cv:   ").append(cfg.getInterarrivalCv()).append("\n")
                    .append("Interarrival stream p:    ").append(cfg.getInterarrivalStreamP()).append("\n")
                    .append("Interarrival stream hexp1: ").append(cfg.getInterarrivalStreamHexp1()).append("\n")
                    .append("Interarrival stream hexp2: ").append(cfg.getInterarrivalStreamHexp2()).append("\n")
                    .append("Service mean:      ").append(cfg.getServiceMean()).append("\n")
                    .append("Service cv:        ").append(cfg.getServiceCv()).append("\n")
                    .append("Service stream p:    ").append(cfg.getServiceStreamP()).append("\n")
                    .append("Service stream hexp1: ").append(cfg.getServiceStreamHexp1()).append("\n")
                    .append("Service stream hexp2: ").append(cfg.getServiceStreamHexp2()).append("\n")

                    .append("\n--- Scheduling ---\n")
                    .append("Policy: ").append(cfg.getSchedulingType()).append("\n")

                    .append("\n--- Scaling: Spike Server ---\n")
                    .append("Enabled:        ").append(cfg.isSpikeEnabled()).append("\n")
                    .append("SImax:          ").append(cfg.getSImax()).append("\n")
                    .append("SImin:          ").append(cfg.getSImin()).append("\n")
                    .append("CPU Multiplier: ").append(cfg.getSpikeCpuMultiplier()).append("\n")
                    .append("CPU Percentage: ").append(cfg.getSpikeCpuPercentage()).append("\n")
                    .append("CoolDown:       ").append(cfg.getSpikeCoolDown()).append("\n")

                    .append("\n--- Scaling: Horizontal ---\n")
                    .append("Enabled:            ").append(cfg.isHorizontalEnabled()).append("\n")
                    .append("SlidingWindowSize:  ").append(cfg.getSlidingWindowSize()).append("\n")
                    .append("R0max:              ").append(cfg.getR0max()).append("\n")
                    .append("R0min:              ").append(cfg.getR0min()).append("\n")
                    .append("CoolDown:           ").append(cfg.getHorizontalCoolDown()).append("\n")
                    .append("InitialServerCount: ").append(cfg.getInitialServerCount()).append("\n")

                    .append("\n--- Output ---\n")
                    .append("CSV Dir:  ").append(cfg.getCsvOutputDir()).append("\n")
                    .append("Plot Dir: ").append(cfg.getPlotOutputDir()).append("\n")
                    .append("===========================\n");

            logger.log(Level.CONFIG,sb.toString());
        }
    }
}
