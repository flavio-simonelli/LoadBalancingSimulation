package it.pmcsn.lbsim;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.controller.SimulatorController;


public class Main {
    private static final String configFilePath = "config.yaml"; // Default configuration file path

    public static void main(String[] args) {

        // Use command line argument for config file, or default to config.yaml
        String configFile = args.length > 0 ? args[0] : configFilePath;
        
        // read the configuration from a YAML file
        SimConfiguration config = ConfigLoader.load(configFile);

        // create the application controller
        SimulatorController controller = new SimulatorController();

        // start the simulation
        controller.startSimulation(config);
    }

}