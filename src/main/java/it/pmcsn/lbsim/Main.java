package it.pmcsn.lbsim;

import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.config.YamlSimulationConfig;
import it.pmcsn.lbsim.controller.SimulatorController;

public class Main {
    private static final String configFilePath = "config.yaml"; // Default configuration file path

    public static void main(String[] args) {

        // read the configuration from a YAML file
        SimConfiguration config = new YamlSimulationConfig(configFilePath);

        // create the application controller
        SimulatorController controller = new SimulatorController();

        // start the simulation
        controller.startSimulation(config);

    }

}