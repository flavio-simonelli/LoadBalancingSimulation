package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.WelfordSimple;

public class WelfordDetailedTest {
    public static void main(String[] args) {
        testWelfordStepByStep();
        testWelfordVarianceFormula();
    }
    
    public static void testWelfordStepByStep() {
        System.out.println("Testing Welford algorithm step by step...");
        
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        WelfordSimple welford = new WelfordSimple();
        
        for (int i = 0; i < data.length; i++) {
            welford.iteration(data[i]);
            System.out.printf("After adding %.1f: mean=%.6f, variance=%.6f, count=%d\n", 
                data[i], welford.getAvg(), welford.getVariance(), welford.getI());
        }
    }
    
    public static void testWelfordVarianceFormula() {
        System.out.println("\nTesting the variance formula specifically...");
        
        // Test the exact formula in the current implementation
        WelfordSimple welford = new WelfordSimple();
        
        // Let's trace through what happens with data [1.0, 5.0]
        // This should have mean = 3.0, variance = 4.0 (population) or 8.0 (sample)
        double[] testData = {1.0, 5.0};
        
        System.out.println("Testing with data [1.0, 5.0]");
        System.out.println("Expected mean: 3.0");
        System.out.println("Expected population variance: 4.0");
        System.out.println("Expected sample variance: 8.0");
        
        // Manually trace the algorithm:
        System.out.println("\nManual trace of current algorithm:");
        
        // First iteration: x=1.0
        int i = 1;
        double avg = 0.0;
        double variance = 0.0;
        double x = 1.0;
        
        double d = x - avg; // d = 1.0 - 0.0 = 1.0
        variance = variance + d * d * (i - 1.0) / i; // variance = 0 + 1.0 * 1.0 * 0/1 = 0
        avg = avg + d / i; // avg = 0 + 1.0/1 = 1.0
        
        System.out.printf("After x=%.1f: i=%d, avg=%.6f, variance=%.6f\n", x, i, avg, variance);
        
        // Second iteration: x=5.0
        i = 2;
        x = 5.0;
        
        d = x - avg; // d = 5.0 - 1.0 = 4.0
        variance = variance + d * d * (i - 1.0) / i; // variance = 0 + 4.0 * 4.0 * 1/2 = 8.0
        avg = avg + d / i; // avg = 1.0 + 4.0/2 = 3.0
        
        System.out.printf("After x=%.1f: i=%d, avg=%.6f, variance=%.6f\n", x, i, avg, variance);
        System.out.printf("Final variance/i = %.6f\n", variance/i);
        
        // Now test with actual class
        System.out.println("\nActual class results:");
        for (double value : testData) {
            welford.iteration(value);
        }
        System.out.printf("Mean: %.6f, Variance: %.6f\n", welford.getAvg(), welford.getVariance());
        
        // The issue might be more subtle - let's check with a larger dataset
        System.out.println("\nTesting with response times similar to M/M/1 queue...");
        testMMOneScenario();
    }
    
    public static void testMMOneScenario() {
        // Simulate some response times from M/M/1 queue with λ=5, μ=6.25
        // Expected mean response time = 1/(μ-λ) = 1/1.25 = 0.8
        
        // Generate some exponential response times with mean 0.8
        double[] responseTimes = {0.1, 0.5, 1.2, 0.3, 2.1, 0.7, 0.9, 1.4, 0.6, 0.2};
        
        // Calculate true mean
        double sum = 0;
        for (double rt : responseTimes) {
            sum += rt;
        }
        double trueMean = sum / responseTimes.length;
        
        System.out.printf("Sample response times mean: %.6f\n", trueMean);
        
        WelfordSimple welford = new WelfordSimple();
        for (double rt : responseTimes) {
            welford.iteration(rt);
        }
        
        System.out.printf("Welford computed mean: %.6f\n", welford.getAvg());
        System.out.printf("Difference: %.6f\n", welford.getAvg() - trueMean);
        
        if (Math.abs(welford.getAvg() - trueMean) > 1e-10) {
            System.out.println("*** POTENTIAL BUG: Welford mean doesn't match true mean! ***");
        }
    }
}