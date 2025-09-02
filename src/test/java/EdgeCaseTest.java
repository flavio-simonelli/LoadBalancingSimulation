import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.simulation.JobStats;

public class EdgeCaseTest {
    
    public static void main(String[] args) {
        System.out.println("=== Edge Case Testing ===");
        
        testZeroSizeJob();
        testVerySmallJob();
        testSimultaneousCompletion();
        testSingleJobCompletion();
    }
    
    static void testZeroSizeJob() {
        System.out.println("\n--- Test: Zero-size job ---");
        Server server = new Server(1.0, 1.0, 0);
        Job job = new Job(0.0);
        
        server.addJob(job);
        job.assignServer(server);
        
        System.out.println("Before processing: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize());
        
        server.processJobs(1.0);
        
        System.out.println("After processing: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize());
        System.out.println("Job completed and removed: " + !server.getActiveJobs().contains(job));
    }
    
    static void testVerySmallJob() {
        System.out.println("\n--- Test: Very small job ---");
        Server server = new Server(1.0, 1.0, 0);
        Job job = new Job(1e-12); // Very small job
        
        server.addJob(job);
        job.assignServer(server);
        
        System.out.println("Before processing: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize());
        
        server.processJobs(1.0);
        
        System.out.println("After processing: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize());
        System.out.println("Job completed and removed: " + !server.getActiveJobs().contains(job));
    }
    
    static void testSimultaneousCompletion() {
        System.out.println("\n--- Test: Multiple jobs completing simultaneously ---");
        Server server = new Server(1.0, 1.0, 0);
        Job job1 = new Job(1.0);
        Job job2 = new Job(1.0); // Same size as job1
        
        server.addJob(job1);
        job1.assignServer(server);
        server.addJob(job2);
        job2.assignServer(server);
        
        System.out.println("Before processing: Server load=" + server.getCurrentSI());
        
        // Process for exactly 2 seconds - both jobs should complete simultaneously
        server.processJobs(2.0);
        
        System.out.println("After processing: Server load=" + server.getCurrentSI());
        System.out.println("Job1 remaining=" + job1.getRemainingSize() + ", Job2 remaining=" + job2.getRemainingSize());
        System.out.println("Both jobs removed: " + (server.getActiveJobs().size() == 0));
    }
    
    static void testSingleJobCompletion() {
        System.out.println("\n--- Test: Single job processor sharing ---");
        Server server = new Server(1.0, 1.0, 0);
        Job job = new Job(2.0);
        
        server.addJob(job);
        job.assignServer(server);
        
        JobStats stats = new JobStats(job, 0.0, 2.0);
        stats.estimateDepartureTime(0.0);
        
        System.out.println("Initial: Server load=" + server.getCurrentSI() + ", Estimated departure=" + stats.getEstimatedDepartureTime());
        
        // Process for 1 second
        server.processJobs(1.0);
        stats.estimateDepartureTime(1.0);
        
        System.out.println("After 1s: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize() + ", Est departure=" + stats.getEstimatedDepartureTime());
        
        // Process for another 1 second to complete
        server.processJobs(1.0);
        
        System.out.println("After 2s: Server load=" + server.getCurrentSI() + ", Job remaining=" + job.getRemainingSize());
        System.out.println("Job completed correctly: " + (server.getCurrentSI() == 0 && Math.abs(job.getRemainingSize()) <= 1e-9));
    }
}