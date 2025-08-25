package it.pmcsn.lbsim.config;

import it.pmcsn.lbsim.models.schedulingpolicy.SchedulingType;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class YamlSimulationConfig implements SimConfiguration {

    private final double duration;
    private final int SImax;
    private final int SImin;
    private final double R0max;
    private final double R0min;
    private final int slidingWindowSize;
    private final int horizonalScalingCoolDown;
    private final int initialServerCount;
    private final int cpuMultiplierSpike;
    private final double cpuPercentageSpike;
    private final SchedulingType schedulingType;
    private final double interarrivalCv;
    private final double interarrivalMean;
    private final double serviceCv;
    private final double serviceMean;
    private final String csvOutputDir;
    private final String plotOutputDir;

    public YamlSimulationConfig(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new RuntimeException("File YAML non trovato: " + filePath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(inputStream);
            Map<String, Object> simConfig = (Map<String, Object>) obj.get("simulazione");
            if (simConfig == null) {
                throw new RuntimeException("Chiave 'simulazione' non trovata nel file YAML");
            }

            this.duration = ((Number) simConfig.get("duration")).doubleValue();
            this.SImax = ((Number) simConfig.get("SImax")).intValue();
            this.SImin = ((Number) simConfig.get("SImin")).intValue();
            this.R0max = ((Number) simConfig.get("R0max")).doubleValue();
            this.R0min = ((Number) simConfig.get("R0min")).doubleValue();
            this.slidingWindowSize = ((Number) simConfig.get("slidingWindowSize")).intValue();
            this.horizonalScalingCoolDown = ((Number) simConfig.get("horizontalScalingCoolDown")).intValue();
            this.initialServerCount = ((Number) simConfig.get("initialServerCount")).intValue();
            this.cpuMultiplierSpike = ((Number) simConfig.get("cpuMultiplierSpike")).intValue();
            this.cpuPercentageSpike = ((Number) simConfig.get("cpuPercentageSpike")).doubleValue();
            this.schedulingType = SchedulingType.fromString((String) simConfig.get("schedulingPolicy"));
            this.interarrivalCv = ((Number) simConfig.get("interarrivalCv")).doubleValue();
            this.interarrivalMean = ((Number) simConfig.get("interarrivalMean")).doubleValue();
            this.serviceCv = ((Number) simConfig.get("serviceCv")).doubleValue();
            this.serviceMean = ((Number) simConfig.get("serviceMean")).doubleValue();
            this.csvOutputDir = (String) simConfig.get("csvOutputDir");
            this.plotOutputDir = (String) simConfig.get("plotOutputDir");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore nel caricamento della configurazione YAML", e);
        }
    }

    @Override
    public double getDuration() { return duration; }
    @Override
    public int getSImax() { return SImax; }
    @Override
    public int getSImin() { return SImin; }
    @Override
    public double getR0max() { return R0max; }
    @Override
    public double getR0min() { return R0min; }
    @Override
    public int getSlidingWindowSize() { return slidingWindowSize; }
    @Override
    public int getHorizonalScalingCoolDown() { return horizonalScalingCoolDown; }
    @Override
    public int getInitialServerCount() { return initialServerCount; }
    @Override
    public int getCpuMultiplierSpike() { return cpuMultiplierSpike; }
    @Override
    public double getCpuPercentageSpike() { return cpuPercentageSpike; }
    @Override
    public SchedulingType getSchedulingType() { return schedulingType; }
    @Override
    public double getInterarrivalCv() { return interarrivalCv; }
    @Override
    public double getInterarrivalMean() { return interarrivalMean; }
    @Override
    public double getServiceCv() { return serviceCv; }
    @Override
    public double getServiceMean() { return serviceMean; }
    @Override
    public String getCsvOutputDir() { return csvOutputDir; }
    @Override
    public String getPlotOutputDir() { return plotOutputDir; }
}
