package it.pmcsn.lbsim.controller;

import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicy;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicyLeastUsed;
import it.pmcsn.lbsim.models.domain.scaling.horizontalscaler.HorizontalScaler;
import it.pmcsn.lbsim.models.domain.scaling.horizontalscaler.NoneHorizontalScaler;
import it.pmcsn.lbsim.models.domain.scaling.horizontalscaler.SlidingWindowHorizontalScaler;
import it.pmcsn.lbsim.models.domain.scaling.spikerouter.NoneSpikeRouter;
import it.pmcsn.lbsim.models.domain.scaling.spikerouter.SimpleSpikeRouter;
import it.pmcsn.lbsim.models.domain.scaling.spikerouter.SpikeRouter;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.LeastLoadPolicy;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingPolicy;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.server.ServerPool;
import it.pmcsn.lbsim.models.simulation.Simulator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.DistributionWorkloadGenerator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.TraceWorkloadGenerator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.WorkloadGenerator;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;


import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimulatorController {

    private static final Logger logger = Logger.getLogger(SimulatorController.class.getName());

    public void startSimulation(SimConfiguration config) {
        if (config == null) {
            logger.log(Level.SEVERE,"Simulation configuration cannot be null");
            throw new IllegalArgumentException("Simulation configuration cannot be null");
        }

        WorkloadGenerator wg;
        Rngs rngs = new Rngs();

        if (config.getIsTracedriven()){
            try {
                wg = new TraceWorkloadGenerator(Path.of(config.getTraceArrivalsPath()), Path.of(config.getTraceSizePath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Initialize random number generators
            if (config.isFirstSimulation()) {
                rngs.plantSeeds(config.getSeed0());
            } else {
                rngs.plantSeeds(-1); // only for initialize
                rngs.selectStream(0);
                rngs.putSeed(config.getSeed0());
                rngs.selectStream(1);
                rngs.putSeed(config.getSeed1());
                rngs.selectStream(2);
                rngs.putSeed(config.getSeed2());
                rngs.selectStream(3);
                rngs.putSeed(config.getSeed3());
                rngs.selectStream(4);
                rngs.putSeed(config.getSeed4());
                rngs.selectStream(5);
                rngs.putSeed(config.getSeed5());
            }
            Rvgs rvgs = new Rvgs(rngs);
            logger.log(Level.INFO, "Initial seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

            HyperExponential interarrivalTimeObj; // Hyperexponential distribution for interarrival times
            HyperExponential serviceTimeObj; // Hyperexponential distribution for service times

            // Initialize hyperexponential distribution
            interarrivalTimeObj = new HyperExponential(config.getInterarrivalCv(), config.getInterarrivalMean(),
                    config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2());
            logger.log(Level.FINE, "Hyperexponential interarrival with parameters {0} {1} {2}\n",
                    new Object[]{interarrivalTimeObj.getP(), interarrivalTimeObj.getM1(), interarrivalTimeObj.getM2()});

            serviceTimeObj = new HyperExponential(config.getServiceCv(), config.getServiceMean(),
                    config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2());
            logger.log(Level.FINE, "Hyperexponential service with parameters {0} {1} {2}\n",
                    new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2()});

            wg = new DistributionWorkloadGenerator(rvgs, interarrivalTimeObj, serviceTimeObj);
        }

        // create the system under simulation
        RemovalPolicy removalPolicy = new RemovalPolicyLeastUsed();
        ServerPool serverPool = new ServerPool(config.getInitialServerCount(), 1.0, removalPolicy);
        Server spikeServer = new Server(config.getSpikeCpuMultiplier(), config.getSpikeCpuPercentage(), -1);
        SchedulingPolicy schedulingPolicy = new LeastLoadPolicy();
        SpikeRouter spikeRouter;
        if (config.isSpikeEnabled()){
            spikeRouter = new SimpleSpikeRouter(config.getSImax());
        }
        else {
            spikeRouter = new NoneSpikeRouter();
        }

        HorizontalScaler horizontalScaler;
        if (config.isHorizontalEnabled()){
            horizontalScaler = new SlidingWindowHorizontalScaler(
                    config.getSlidingWindowSize(),
                    config.getR0min().getSeconds(),
                    config.getR0max().getSeconds(),
                    config.getHorizontalCoolDown().getSeconds());
        }else {
            horizontalScaler = new NoneHorizontalScaler();
        }
        LoadBalancer loadBalancer = new LoadBalancer(
                serverPool,
                spikeServer,
                schedulingPolicy,
                spikeRouter,
                horizontalScaler
        );

        // initialize csv appender
        CsvAppender csvAppenderJobs;
        CsvAppender csvAppenderServers;

        try {
            csvAppenderJobs = new CsvAppender(Path.of(config.getCsvOutputDir() + "Jobs.csv"), "IdJob", "Arrival", "Departure", "ResponseTime","Response-(Departure-Arrival)", "OriginalSize", "processedBySpike");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            csvAppenderServers = new CsvAppender(Path.of(config.getCsvOutputDir() + "Servers.csv"), "timestamp", "active_jobs_per_webserver", "active_web_servers", "active_jobs_spikeserver");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



        // Create a new simulator instance with the provided configuration
        Simulator simulator = new Simulator(
                wg,
                loadBalancer,
                csvAppenderServers,
                csvAppenderJobs);

        // Start the simulation
        simulator.run(config.getDurationSeconds().getSeconds());

        // print final seed
        logger.log(Level.INFO, "Final seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

        csvAppenderJobs.close();
        csvAppenderServers.close();
    }
}

