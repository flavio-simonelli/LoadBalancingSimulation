package it.pmcsn.lbsim.controller;

import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.models.simulation.Simulator;

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

        final HyperExponential interarrivalTimeObj; // Hyperexponential distribution for interarrival times
        final HyperExponential serviceTimeObj; // Hyperexponential distribution for service times

        // Initialize hyperexponential distribution
        interarrivalTimeObj = new HyperExponential(config.getInterarrivalCv(), config.getInterarrivalMean(),
                config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2());
        logger.log(Level.FINE, "Hyperexponential interarrival with parameters {0} {1} {2}\n",
                new Object[]{interarrivalTimeObj.getP(), interarrivalTimeObj.getM1(), interarrivalTimeObj.getM2()});

        serviceTimeObj = new HyperExponential(config.getServiceCv(), config.getServiceMean(),
                config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2());
        logger.log(Level.FINE, "Hyperexponential service with parameters {0} {1} {2}\n",
                new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2()});

        // Create a new simulator instance with the provided configuration
        Simulator simulator = new Simulator(config.isFirstSimulation(),
                                            config.getSeed0(),
                                            config.getSeed1(),
                                            config.getSeed2(),
                                            config.getSeed3(),
                                            config.getSeed4(),
                                            config.getSeed5(),
                                            config.getSImax(),
                                            config.getSImin(),
                                            config.getR0max().getSeconds(),
                                            config.getR0min().getSeconds(),
                                            config.getInitialServerCount(),
                                            config.getSpikeCpuMultiplier(),
                                            config.getSpikeCpuPercentage(),
                                            config.getSlidingWindowSize(),
                                            config.getSchedulingType(),
                                            config.getHorizontalCoolDown().getSeconds(),
                                            config.getInterarrivalCv(),
                                            config.getInterarrivalMean(),
                                            config.getServiceCv(),
                                            config.getServiceMean(),
                                            config.getCsvOutputDir()
                                            );

        // Start the simulation
        simulator.run(config.getDurationSeconds().getSeconds());
    }
}

