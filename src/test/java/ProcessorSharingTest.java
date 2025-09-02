import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.server.Server;

public class ProcessorSharingTest {
    
    public static void main(String[] args) {
        System.out.println("=== Processor Sharing Test ===");
        
        // Create a server with CPU multiplier 1 and CPU percentage 1.0
        Server server = new Server(1.0, 1.0, 0);
        
        // Create two jobs with different sizes
        Job job1 = new Job(1.0); // 1 second job
        Job job2 = new Job(2.0); // 2 second job
        
        System.out.println("Initial server load: " + server.getCurrentSI());
        System.out.println("Job1 remaining size: " + job1.getRemainingSize());
        System.out.println("Job2 remaining size: " + job2.getRemainingSize());
        
        // Assign jobs to server
        server.addJob(job1);
        job1.assignServer(server);
        server.addJob(job2);
        job2.assignServer(server);
        
        System.out.println("\nAfter adding jobs:");
        System.out.println("Server load: " + server.getCurrentSI());
        System.out.println("Active jobs: " + server.getActiveJobs().size());
        
        // Process jobs for 1 second
        // With processor sharing, each job should get 0.5 CPU time
        // So in 1 second, each job should be processed by 0.5 seconds
        System.out.println("\n=== Processing jobs for 1.0 seconds ===");
        server.processJobs(1.0);
        
        System.out.println("After processing:");
        System.out.println("Server load: " + server.getCurrentSI());
        System.out.println("Active jobs: " + server.getActiveJobs().size());
        System.out.println("Job1 remaining size: " + job1.getRemainingSize());
        System.out.println("Job2 remaining size: " + job2.getRemainingSize());
        
        // Expected: Job1 should have 0.5 remaining (1.0 - 0.5), Job2 should have 1.5 remaining (2.0 - 0.5)
        // But if there's a bug, completed jobs might not be removed
        
        // Process another 1 second
        System.out.println("\n=== Processing jobs for another 1.0 seconds ===");
        server.processJobs(1.0);
        
        System.out.println("After second processing:");
        System.out.println("Server load: " + server.getCurrentSI());
        System.out.println("Active jobs: " + server.getActiveJobs().size());
        System.out.println("Job1 remaining size: " + job1.getRemainingSize());
        System.out.println("Job2 remaining size: " + job2.getRemainingSize());
        
        // Check if completed jobs are still in the list
        System.out.println("\n=== Checking job completion ===");
        for (Job job : server.getActiveJobs()) {
            System.out.println("Job " + job.getJobId() + " remaining: " + job.getRemainingSize());
            if (job.getRemainingSize() <= 1e-9) {
                System.out.println("  -> This job should be completed!");
            }
        }
    }
}