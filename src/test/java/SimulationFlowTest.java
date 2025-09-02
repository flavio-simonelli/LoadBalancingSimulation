import it.pmcsn.lbsim.models.domain.Job;
import it.pmcsn.lbsim.models.domain.server.Server;
import it.pmcsn.lbsim.models.simulation.JobStats;

public class SimulationFlowTest {
    
    public static void main(String[] args) {
        System.out.println("=== Simulation Flow Test ===");
        
        // Create server
        Server server = new Server(1.0, 1.0, 0);
        
        // Create a job
        Job job1 = new Job(1.0);
        
        // Assign job (like in arrival handler)
        server.addJob(job1);
        job1.assignServer(server);
        
        // Create JobStats (like in arrival handler)
        JobStats jobStats = new JobStats(job1, 0.0, 1.0);
        jobStats.estimateDepartureTime(0.0);
        
        System.out.println("At t=0.0:");
        System.out.println("  Server load: " + server.getCurrentSI());
        System.out.println("  Job remaining: " + job1.getRemainingSize());
        System.out.println("  Estimated departure: " + jobStats.getEstimatedDepartureTime());
        
        // Process job for 0.5 seconds (like in arrival/departure handler)
        System.out.println("\n=== Processing for 0.5 seconds ===");
        server.processJobs(0.5);
        
        System.out.println("After processing 0.5s:");
        System.out.println("  Server load: " + server.getCurrentSI());
        System.out.println("  Job remaining: " + job1.getRemainingSize());
        
        // Recalculate departure time (like in arrival/departure handler)
        jobStats.estimateDepartureTime(0.5);
        System.out.println("  New estimated departure: " + jobStats.getEstimatedDepartureTime());
        
        // Process another 0.5 seconds to complete the job
        System.out.println("\n=== Processing for another 0.5 seconds ===");
        server.processJobs(0.5);
        
        System.out.println("After processing another 0.5s:");
        System.out.println("  Server load: " + server.getCurrentSI());
        System.out.println("  Job remaining: " + job1.getRemainingSize());
        System.out.println("  Is job completed? " + (Math.abs(job1.getRemainingSize()) <= 1e-9));
        
        // This is the problem: the job is completed but still counted in server load
        // When we try to recalculate departure times, the server load is wrong
        
        // Try to recalculate departure time - this should cause issues if job is done
        try {
            jobStats.estimateDepartureTime(1.0);
            System.out.println("  New estimated departure: " + jobStats.getEstimatedDepartureTime());
        } catch (Exception e) {
            System.out.println("  Error recalculating departure: " + e.getMessage());
        }
        
        // Simulate the completeJob process
        System.out.println("\n=== Simulating departure event handling ===");
        try {
            server.removeJob(job1);
            System.out.println("Job successfully removed from server");
            System.out.println("  Final server load: " + server.getCurrentSI());
        } catch (Exception e) {
            System.out.println("Error removing job: " + e.getMessage());
        }
    }
}