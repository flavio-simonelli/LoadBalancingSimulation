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
import it.pmcsn.lbsim.models.domain.schedulingpolicy.RoundRobinPolicy;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingPolicy;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.server.ServerPool;
import it.pmcsn.lbsim.models.simulation.Simulator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.*;
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

        WorkloadGenerator wg = null;
        Rngs rngs = new Rngs();

        for (int i=0; i< config.getReplications(); i++) {
            if (i == 0) {
                rngs.plantSeeds(config.getSeed0());
            }
            Rvgs rvgs = new Rvgs(rngs);
            logger.log(Level.INFO, "Initial seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));
            HyperExponential interarrivalTimeObj;
            HyperExponential serviceTimeObj;


            switch (config.getChooseWorkload()){
                case WorkloadType.HYPEREXPONENTIAL:
                    interarrivalTimeObj = new HyperExponential(config.getInterarrivalCv(), config.getInterarrivalMean(),
                            config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2());
                    logger.log(Level.INFO, "Hyperexponential interarrival with parameters {0} {1} {2} and {3} {4} {5}\n",
                            new Object[]{interarrivalTimeObj.getP(), interarrivalTimeObj.getM1(), interarrivalTimeObj.getM2(), interarrivalTimeObj.getStreamP(), interarrivalTimeObj.getStreamExp1(), interarrivalTimeObj.getStreamExp2()});
                    serviceTimeObj = new HyperExponential(config.getServiceCv(), config.getServiceMean(),
                            config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2());
                    logger.log(Level.INFO, "Hyperexponential service with parameters {0} {1} {2} and stream {3} {4} {5}\n",
                            new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2(), serviceTimeObj.getStreamP(), serviceTimeObj.getStreamExp1(), serviceTimeObj.getStreamExp2()});
                    wg = new DistributionWorkloadGenerator(rvgs, interarrivalTimeObj, serviceTimeObj);
                    break;
                case WorkloadType.EXPONENTIAL:
                    serviceTimeObj = new HyperExponential(config.getServiceCv(), config.getServiceMean(),
                            config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2());
                    logger.log(Level.INFO, "Exponential interarrival with mean {0}\n", new Object[]{config.getInterarrivalMean()});
                    logger.log(Level.INFO, "Hyperexponential service with parameters {0} {1} {2} and stream {3} {4} {5}\n", new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2(), serviceTimeObj.getStreamP(), serviceTimeObj.getStreamExp1(), serviceTimeObj.getStreamExp2()});
                    wg = new VerifyWorkloadGenerator(rvgs, config.getInterarrivalMean(), serviceTimeObj);
                    break;
                case WorkloadType.TRACE:
                    try {
                        logger.log(Level.INFO, "Trace driven workload with arrivals from {0} and sizes from {1}\n", new Object[]{config.getTraceArrivalsPath(), config.getTraceSizePath()});
                        wg = new TraceWorkloadGenerator(Path.of(config.getTraceArrivalsPath()), Path.of(config.getTraceSizePath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
            // create the system under simulation
            RemovalPolicy removalPolicy = new RemovalPolicyLeastUsed();
            ServerPool serverPool = new ServerPool(config.getInitialServerCount(), 1.0, removalPolicy);
            Server spikeServer = new Server(config.getSpikeCpuMultiplier(), config.getSpikeCpuPercentage(), -1);
            SchedulingPolicy schedulingPolicy = switch (config.getSchedulingType()) {
                case LEAST_LOAD -> new LeastLoadPolicy();
                case ROUND_ROBIN -> new RoundRobinPolicy();
                default -> {
                    logger.log(Level.SEVERE, "Unsupported scheduling policy: {0}\n", config.getSchedulingType());
                    throw new IllegalArgumentException("Unsupported scheduling policy: " + config.getSchedulingType());
                }
            };
            SpikeRouter spikeRouter;
            if (config.isSpikeEnabled()) {
                spikeRouter = new SimpleSpikeRouter(config.getSImax());
            } else {
                spikeRouter = new NoneSpikeRouter();
            }

            HorizontalScaler horizontalScaler;
            if (config.isHorizontalEnabled()) {
                horizontalScaler = new SlidingWindowHorizontalScaler(
                        config.getSlidingWindowSize(),
                        config.getR0min().getSeconds(),
                        config.getR0max().getSeconds(),
                        config.getHorizontalCoolDown().getSeconds());
            } else {
                horizontalScaler = new NoneHorizontalScaler();
            }
            LoadBalancer loadBalancer = new LoadBalancer(
                    serverPool,
                    spikeServer,
                    schedulingPolicy,
                    spikeRouter,
                    horizontalScaler
            );
            CsvAppender welfordCsv;
            rngs.selectStream(0);
            try {
                welfordCsv = new CsvAppender(Path.of("output/csv/Welford"+rngs.getSeed()+"rep"+i+".csv"),"Type","N","Mean","StdDev","Variance", "Semi Intervallo media");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // Create a new simulator instance with the provided configuration
            Simulator simulator = new Simulator(
                    wg,
                    loadBalancer,
                    welfordCsv
            );

            // Start the simulation
            simulator.run(config.getDurationSeconds().getSeconds());

            // print final seed
            logger.log(Level.INFO, "Final seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));

            welfordCsv.close();
        }
    }
}

