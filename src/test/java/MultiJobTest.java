import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.simulation.JobStats;

public class MultiJobTest {
    
    public static void main(String[] args) {
        System.out.println("=== Multi-Job Processor Sharing Test ===");
        
        Server server = new Server(1.0, 1.0, 0);
        
        // Create jobs with different sizes
        Job job1 = new Job(1.0);  // Will complete first
        Job job2 = new Job(3.0);  // Will complete later
        
        server.addJob(job1);
        job1.assignServer(server);
        server.addJob(job2);
        job2.assignServer(server);
        
        JobStats stats1 = new JobStats(job1, 0.0, 1.0);
        JobStats stats2 = new JobStats(job2, 0.0, 3.0);
        
        System.out.println("Initial state:");
        System.out.println("  Server load: " + server.getCurrentSI());
        System.out.println("  Job1 remaining: " + job1.getRemainingSize());
        System.out.println("  Job2 remaining: " + job2.getRemainingSize());
        
        // Estimate departure times
        stats1.estimateDepartureTime(0.0);
        stats2.estimateDepartureTime(0.0);
        System.out.println("  Job1 estimated departure: " + stats1.getEstimatedDepartureTime());
        System.out.println("  Job2 estimated departure: " + stats2.getEstimatedDepartureTime());
        
        // Process for 1 second - each job should get 0.5 CPU time
        System.out.println("\n=== Processing for 1.0 seconds ===");
        server.processJobs(1.0);
        
        System.out.println("After 1.0s processing:");
        System.out.println("  Server load: " + server.getCurrentSI()); // Should still be 2
        System.out.println("  Job1 remaining: " + job1.getRemainingSize()); // Should be 0.5
        System.out.println("  Job2 remaining: " + job2.getRemainingSize()); // Should be 2.5
        
        // Process for another 1 second
        System.out.println("\n=== Processing for another 1.0 seconds ===");
        server.processJobs(1.0);
        
        System.out.println("After 2.0s total processing:");
        System.out.println("  Server load: " + server.getCurrentSI()); // Should still be 2 - THIS IS THE BUG!
        System.out.println("  Job1 remaining: " + job1.getRemainingSize()); // Should be 0.0 (completed)
        System.out.println("  Job2 remaining: " + job2.getRemainingSize()); // Should be 2.0
        
        // The problem: Job1 is completed but still in activeJobs
        // This affects departure time estimation for Job2
        System.out.println("\n=== Departure time estimation with completed job in list ===");
        stats2.estimateDepartureTime(2.0);
        System.out.println("  Job2 estimated departure: " + stats2.getEstimatedDepartureTime());
        // This will be WRONG because it uses server load = 2 instead of 1
        
        // Remove the completed job
        System.out.println("\n=== Removing completed job ===");
        try {
            server.removeJob(job1);
            System.out.println("Job1 removed successfully");
            System.out.println("  Server load after removal: " + server.getCurrentSI()); // Should be 1
            
            // Now recalculate Job2's departure time
            stats2.estimateDepartureTime(2.0);
            System.out.println("  Job2 correct estimated departure: " + stats2.getEstimatedDepartureTime());
            
        } catch (Exception e) {
            System.out.println("Error removing job1: " + e.getMessage());
        }
    }
}