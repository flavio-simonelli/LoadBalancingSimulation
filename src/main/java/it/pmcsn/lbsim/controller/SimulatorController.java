package it.pmcsn.lbsim.controller;

import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.config.YamlSimulationConfig;
import it.pmcsn.lbsim.models.Job;
import it.pmcsn.lbsim.models.Simulator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimulatorController {

    private static final Logger logger = Logger.getLogger(SimulatorController.class.getName());

    public SimulatorController() {}

    public void startSimulation(SimConfiguration config) {
        if (config == null) {
            logger.log(Level.SEVERE,"Simulation configuration cannot be null");
            throw new IllegalArgumentException("Simulation configuration cannot be null");
        }
        // Create a new simulator instance with the provided configuration
        Simulator simulator = new Simulator(config.getSImax(),
                                            config.getSImin(),
                                            config.getR0max(),
                                            config.getR0min(),
                                            config.getInitialServerCount(),
                                            config.getCpuMultiplierSpike(),
                                            config.getCpuPercentageSpike(),
                                            config.getSlidingWindowSize(),
                                            config.getSchedulingType(),
                                            config.getHorizonalScalingCoolDown()
                                            );
        // Start the simulation
        simulator.run(config.getDuration());
    }
}

