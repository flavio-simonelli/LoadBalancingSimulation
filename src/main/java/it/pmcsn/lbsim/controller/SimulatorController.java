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
import it.pmcsn.lbsim.models.domain.schedulingpolicy.SchedulingType;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.server.ServerPool;
import it.pmcsn.lbsim.models.simulation.runType.*;
import it.pmcsn.lbsim.models.simulation.Simulator;
import it.pmcsn.lbsim.models.simulation.workloadgenerator.*;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;


import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimulatorController {

    private static final Logger logger = Logger.getLogger(SimulatorController.class.getName());

    public void startSimulation(SimConfiguration config) {
        if (config == null) {
            logger.log(Level.SEVERE, "Simulation configuration cannot be null");
            throw new IllegalArgumentException("Simulation configuration cannot be null");
        }
        switch (config.getRunType()) {
            case INFINITESIMULATION -> infiniteSimualtion(config);
            case FINITESIMULATIONJOBS -> finiteSimulation(config);
            case FINITESIMULATIONTIME -> finiteSimulation(config);
            case AUTOCORRELATION -> autocorrelation(config);
        }
    }

    public void infiniteSimualtion(SimConfiguration config) {
        // istance random number generator and plant the seed
        Rngs rngs = istanceRandomGenerator(config.getSeed());
        // istance workload
        WorkloadGenerator wg = istanceWorkloadGenerator(rngs, config.getChooseWorkload(), config.getInterarrivalMean(), config.getInterarrivalCv(), config.getServiceMean(), config.getServiceCv(), config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2(), config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2(), config.getTraceArrivalsPath(), config.getTraceSizePath());
        // print the initial seed of the replica
        logger.log(Level.INFO, "Initial seeds of the run: {0} \n", Arrays.toString(rngs.getSeedArray()));
        // create a runtype
        RunPolicy runPolicy = new BatchMeans(config.getBatchSize(), 0.95F);
        // create a new system
        Simulator simulator = createNewSimulator(config.getInitialServerCount(), config.getSpikeCpuMultiplier(), config.getSpikeCpuPercentage(), config.getSchedulingType(), config.isSpikeEnabled(), config.getSImax(), config.isHorizontalEnabled(), config.getSlidingWindowSize(), config.getR0min(), config.getR0max(), config.getHorizontalCoolDown(), runPolicy, wg);
        // run simulation
        simulator.run(config.getBatchSize() * config.getNumberOfBatchs());
        logger.log(Level.INFO, "Final seeds of the run {0}\n", Arrays.toString(rngs.getSeedArray()));
        // close csv
        runPolicy.closeCsvs();

    }

    public void finiteSimulation(SimConfiguration config) {
        // istance random number generator and plant the seed
        Rngs rngs = istanceRandomGenerator(config.getSeed());
        // istance workload
        WorkloadGenerator wg = istanceWorkloadGenerator(rngs, config.getChooseWorkload(), config.getInterarrivalMean(), config.getInterarrivalCv(), config.getServiceMean(), config.getServiceCv(), config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2(), config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2(), config.getTraceArrivalsPath(), config.getTraceSizePath());
        // create a run policy
        Replication runPolicy = new Replication(0.95F);
        // run n replicas
        for (int replica = 0; replica<config.getNumberOfReplicas(); replica++) {
            // print the initial seed of the replica
            logger.log(Level.INFO, "Initial seeds of Replica {0}: {1}\n", new Object[]{replica, Arrays.toString(rngs.getSeedArray())});
            // create a new system
            Simulator simulator = createNewSimulator(config.getInitialServerCount(), config.getSpikeCpuMultiplier(), config.getSpikeCpuPercentage(), config.getSchedulingType(), config.isSpikeEnabled(), config.getSImax(), config.isHorizontalEnabled(), config.getSlidingWindowSize(), config.getR0min(), config.getR0max(), config.getHorizontalCoolDown(), runPolicy, wg);
            // run simulation
            if (config.getRunType() == RunType.FINITESIMULATIONTIME){
                simulator.run(config.getDurationInSeconds().getSeconds());
            } else {
                simulator.run(config.getDurationInJobs());
            }
            // print the final seed of the replica
            logger.log(Level.INFO, "Final seeds of Replica {0}: {1}\n", new Object[]{replica,Arrays.toString(rngs.getSeedArray())});
            // update the replica counter
            runPolicy.nextReplica();
        }
        // close csv
        runPolicy.closeCsvs();
        // print the results of the replicas
        runPolicy.finalCalculation();
    }

    public void autocorrelation(SimConfiguration config) {
        // istance random number generator and plant the seed
        Rngs rngs = istanceRandomGenerator(config.getSeed());
        // istance workload
        WorkloadGenerator wg = istanceWorkloadGenerator(rngs, config.getChooseWorkload(), config.getInterarrivalMean(), config.getInterarrivalCv(), config.getServiceMean(), config.getServiceCv(), config.getInterarrivalStreamP(), config.getInterarrivalStreamHexp1(), config.getInterarrivalStreamHexp2(), config.getServiceStreamP(), config.getServiceStreamHexp1(), config.getServiceStreamHexp2(), config.getTraceArrivalsPath(), config.getTraceSizePath());
        // print the initial seed of the replica
        logger.log(Level.INFO, "Initial seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));
        // create a runtype
        RunPolicy runPolicy = new Autocorrelation(config.getMaxLag());
        // create a new system
        Simulator simulator = createNewSimulator(config.getInitialServerCount(), config.getSpikeCpuMultiplier(), config.getSpikeCpuPercentage(), config.getSchedulingType(), config.isSpikeEnabled(), config.getSImax(), config.isHorizontalEnabled(), config.getSlidingWindowSize(), config.getR0min(), config.getR0max(), config.getHorizontalCoolDown(), runPolicy, wg);
        // run simulation
        simulator.run(config.getMaxLag()+500);
        logger.log(Level.INFO, "Final seeds: {0}\n", Arrays.toString(rngs.getSeedArray()));
        // close csv
        runPolicy.closeCsvs();

    }

    public Rngs istanceRandomGenerator(long seed) {
        // create random generator
        Rngs rngs = new Rngs();
        // plant seed
        rngs.plantSeeds(seed);
        return rngs;
    }

    public WorkloadGenerator istanceWorkloadGenerator(Rngs rngs, WorkloadType workloadType, double interarrivalMean, double interarrivalCv, double sizeMean, double sizeCv, int interarrivalStream1, int interarrivalStream2, int interarrivalStream3, int sizeStream1, int sizeStream2, int sizeStream3, String traceArrivalPath, String traceSizePath) {
        WorkloadGenerator wg;
        HyperExponential interarrivalTimeObj;
        HyperExponential serviceTimeObj;
        switch (workloadType) {
            case WorkloadType.HYPEREXPONENTIAL:
                interarrivalTimeObj = new HyperExponential(interarrivalCv, interarrivalMean, interarrivalStream1, interarrivalStream2, interarrivalStream3);
                logger.log(Level.INFO, "Hyperexponential interarrival with parameters {0} {1} {2} and {3} {4} {5}\n", new Object[]{interarrivalTimeObj.getP(), interarrivalTimeObj.getM1(), interarrivalTimeObj.getM2(), interarrivalTimeObj.getStreamP(), interarrivalTimeObj.getStreamExp1(), interarrivalTimeObj.getStreamExp2()});
                serviceTimeObj = new HyperExponential(sizeCv, sizeMean, sizeStream1, sizeStream2, sizeStream3);
                logger.log(Level.INFO, "Hyperexponential service with parameters {0} {1} {2} and stream {3} {4} {5}\n", new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2(), serviceTimeObj.getStreamP(), serviceTimeObj.getStreamExp1(), serviceTimeObj.getStreamExp2()});
                wg = new DistributionWorkloadGenerator(rngs, interarrivalTimeObj, serviceTimeObj);
                break;
            case WorkloadType.EXPONENTIAL:
                serviceTimeObj = new HyperExponential(sizeCv, sizeMean, sizeStream1, sizeStream2, sizeStream3);
                logger.log(Level.INFO, "Exponential interarrival with parameters {0} and stream {1}", new Object[]{interarrivalMean, interarrivalStream1});
                logger.log(Level.INFO, "Hyperexponential service with parameters {0} {1} {2} and stream {3} {4} {5}\n", new Object[]{serviceTimeObj.getP(), serviceTimeObj.getM1(), serviceTimeObj.getM2(), serviceTimeObj.getStreamP(), serviceTimeObj.getStreamExp1(), serviceTimeObj.getStreamExp2()});
                wg = new VerifyWorkloadGenerator(rngs, interarrivalMean, interarrivalStream1, serviceTimeObj);
                break;
            case WorkloadType.FULLEXP:
                logger.log(Level.INFO, "Exponential interarrival with parameters {0} and stream {1}", new Object[]{interarrivalMean, interarrivalStream1});
                logger.log(Level.INFO, "Exponential service with parameters {0} and stream {1}", new Object[]{sizeMean, sizeStream1});
                wg = new FullExpWorkloadGenerator(rngs, interarrivalMean, interarrivalStream1, sizeMean, sizeStream1);
                break;
            case WorkloadType.TRACE:
                try {
                    logger.log(Level.INFO, "Trace driven workload with arrivals from {0} and sizes from {1}\n", new Object[]{traceArrivalPath, traceSizePath});
                    wg = new TraceWorkloadGenerator(Path.of(traceArrivalPath), Path.of(traceSizePath));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                logger.log(Level.SEVERE, "Unsupported workload type: {0}\n", workloadType);
                throw new IllegalArgumentException("Unsupported workload type: " + workloadType);
        }
        return wg;
    }

    public Simulator createNewSimulator(int initialNumberOfWS, double cpuMultiplierSpike, double cpuPercentageSpike, SchedulingType schedulingType, boolean spikeEnable, int SImax, boolean horizontalEnable, int slidingWindowSize, Duration R0min, Duration R0max, Duration horizontalCoolDown, RunPolicy runPolicy, WorkloadGenerator wg) {
        RemovalPolicy removalPolicy = new RemovalPolicyLeastUsed();
        ServerPool serverPool = new ServerPool(initialNumberOfWS, 1.0, removalPolicy);
        Server spikeServer = new Server(cpuMultiplierSpike, cpuPercentageSpike, -1);
        SchedulingPolicy schedulingPolicy = switch (schedulingType) {
            case LEAST_LOAD -> new LeastLoadPolicy();
            case ROUND_ROBIN -> new RoundRobinPolicy();
        };
        SpikeRouter spikeRouter;
        if (spikeEnable) {
            logger.log(Level.INFO, "Spike router enabled");
            spikeRouter = new SimpleSpikeRouter(SImax);
        } else {
            logger.log(Level.INFO, "Spike router disabled");
            spikeRouter = new NoneSpikeRouter();
        }
        HorizontalScaler horizontalScaler;
        if (horizontalEnable) {
            logger.log(Level.INFO, "Horizontal scaler enabled");
            horizontalScaler = new SlidingWindowHorizontalScaler( slidingWindowSize, R0min.getSeconds(), R0max.getSeconds(), horizontalCoolDown.getSeconds());
        } else {
            logger.log(Level.INFO, "Horizontal scaler disabled");
            horizontalScaler = new NoneHorizontalScaler();
        }
        LoadBalancer loadBalancer = new LoadBalancer(
                serverPool,
                spikeServer,
                schedulingPolicy,
                spikeRouter,
                horizontalScaler
        );
        // Create a new simulator instance with the provided configuration
        return new Simulator( wg, loadBalancer, runPolicy);
    }
}
