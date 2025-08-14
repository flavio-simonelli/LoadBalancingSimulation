package it.pmcsn.lbsim.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class YamlSimulationConfig implements SimConfiguration {

    private double duration;
    private int SImax;
    private int SImin;
    private double R0max;
    private double R0min;
    private int slidingWindowSize;
    private int initialServerCount;
    private int cpuMultiplierSpike;
    private double cpuPercentageSpike;

    public YamlSimulationConfig(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new RuntimeException("File YAML non trovato: " + filePath);
            }
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(inputStream);

            // Legge la sezione "simulazione" dal YAML
            Map<String, Object> simConfig = (Map<String, Object>) obj.get("simulazione");
            if (simConfig == null) {
                throw new RuntimeException("Chiave 'simulazione' non trovata nel file YAML");
            }

            // Assegna il valore della durata
            this.duration = ((Number) simConfig.get("duration")).doubleValue();
            this.SImax = ((Number) simConfig.get("SImax")).intValue();
            this.SImin = ((Number) simConfig.get("SImin")).intValue();
            this.R0max = ((Number) simConfig.get("R0max")).doubleValue();
            this.R0min = ((Number) simConfig.get("R0min")).doubleValue();
            this.slidingWindowSize = ((Number) simConfig.get("slidingWindowSize")).intValue();
            this.initialServerCount = ((Number) simConfig.get("initialServerCount")).intValue();
            this.cpuMultiplierSpike = ((Number) simConfig.get("cpuMultiplierSpike")).intValue();
            this.cpuPercentageSpike = ((Number) simConfig.get("cpuPercentageSpike")).doubleValue();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore nel caricamento della configurazione YAML", e);
        }
    }

    @Override
    public double getDuration() {
        return duration;
    }
    @Override
    public int getSImax() {
        return SImax;
    }
    @Override
    public int getSImin() {
        return SImin;
    }
    @Override
    public double getR0max() {
        return R0max;
    }
    @Override
    public double getR0min() {
        return R0min;
    }
    @Override
    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }
    @Override
    public int getInitialServerCount() {
        return initialServerCount;
    }
    @Override
    public int getCpuMultiplierSpike() {
        return cpuMultiplierSpike;
    }
    @Override
    public double getCpuPercentageSpike() {
        return cpuPercentageSpike;
    }
}
