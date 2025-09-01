package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.config.ConfigLoader;
import it.pmcsn.lbsim.config.SimConfiguration;
import it.pmcsn.lbsim.models.domain.LoadBalancer;
import it.pmcsn.lbsim.models.domain.removalPolicy.RemovalPolicyLeastUsed;
import it.pmcsn.lbsim.models.domain.scaling.horizontalscaler.NoneHorizontalScaler;
import it.pmcsn.lbsim.models.domain.scaling.spikerouter.NoneSpikeRouter;
import it.pmcsn.lbsim.models.domain.schedulingpolicy.LeastLoadPolicy;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.domain.server.ServerPool;
import it.pmcsn.lbsim.models.simulation.Simulator;
import it.pmcsn.lbsim.utils.csv.CsvAppender;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test to validate the simulation with true M/M/1 queue
 */
public class MM1ValidationTest {
    private static final Logger logger = Logger.getLogger(MM1ValidationTest.class.getName());

    public static void main(String[] args) {
        System.out.println("M/M/1 Queue Validation Test");
        System.out.println("===========================");
        
        // Test parameters for M/M/1
        double meanInterarrival = 0.2;  // λ = 1/0.2 = 5
        double meanService = 0.16;      // μ = 1/0.16 = 6.25
        double rho = (1.0/meanInterarrival) / (1.0/meanService);
        double theoreticalMeanResponseTime = 1.0 / ((1.0/meanService) - (1.0/meanInterarrival));
        
        System.out.printf("Theoretical parameters:\n");
        System.out.printf("λ = %.3f arrivals/sec\n", 1.0/meanInterarrival);
        System.out.printf("μ = %.3f services/sec\n", 1.0/meanService);
        System.out.printf("ρ = λ/μ = %.3f\n", rho);
        System.out.printf("Expected mean response time = %.6f seconds\n", theoreticalMeanResponseTime);
        System.out.println();
        
        // Set up RNGs
        Rngs rngs = new Rngs();
        rngs.plantSeeds(12345678);
        Rvgs rvgs = new Rvgs(rngs);
        
        // Create truly exponential workload generator
        TrueExponentialWorkloadGenerator wg = new TrueExponentialWorkloadGenerator(
            rvgs, meanInterarrival, meanService);
        
        // Create simple load balancer with 1 server
        ServerPool serverPool = new ServerPool(1, 1.0, new RemovalPolicyLeastUsed());
        Server spikeServer = new Server(1.0, 1.0, -1);
        LoadBalancer loadBalancer = new LoadBalancer(
            serverPool,
            spikeServer,
            new LeastLoadPolicy(),
            new NoneSpikeRouter(),
            new NoneHorizontalScaler()
        );
        
        // Create CSV writers
        try {
            CsvAppender welfordCsv = new CsvAppender(
                Path.of("/tmp/mm1_welford_test.csv"),
                "Type", "N", "Mean", "StdDev", "Variance", "Semi Intervallo media"
            );
            CsvAppender jobStatsCsv = new CsvAppender(
                Path.of("/tmp/mm1_jobs_test.csv"),
                "JobID", "Arrival", "Departure", "ResponseTime", "OriginalSize", "AssignedServerID"
            );
            CsvAppender serverStatsCsv = new CsvAppender(
                Path.of("/tmp/mm1_server_test.csv"),
                "Time", "ServerID", "State", "CpuPercentage", "CpuMultiplier", "CurrentLoad", "JobsInQueue"
            );
            
            // Create simulator
            Simulator simulator = new Simulator(wg, loadBalancer, welfordCsv, jobStatsCsv, serverStatsCsv);
            
            // Run for 1 hour = 3600 seconds
            System.out.println("Running simulation for 1 hour...");
            simulator.run(3600.0);
            
            // Close CSV files
            welfordCsv.close();
            jobStatsCsv.close();
            serverStatsCsv.close();
            
            System.out.println("Simulation completed. Results saved to /tmp/mm1_*.csv");
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}