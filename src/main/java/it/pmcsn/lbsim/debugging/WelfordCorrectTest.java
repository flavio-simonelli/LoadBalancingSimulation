package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.WelfordSimple;

/**
 * Test to demonstrate the bug in WelfordSimple by comparing with a correct implementation
 */
public class WelfordCorrectTest {
    
    static class CorrectWelford {
        private int n = 0;
        private double mean = 0.0;
        private double M2 = 0.0;  // sum of squared differences from mean
        
        public void update(double x) {
            n++;
            double delta = x - mean;
            mean = mean + delta / n;
            double delta2 = x - mean;  // difference from NEW mean
            M2 = M2 + delta * delta2;
        }
        
        public double getMean() { return mean; }
        public double getPopulationVariance() { return n > 0 ? M2 / n : 0.0; }
        public double getSampleVariance() { return n > 1 ? M2 / (n - 1) : 0.0; }
        public int getCount() { return n; }
    }
    
    public static void main(String[] args) {
        System.out.println("Comparing WelfordSimple (buggy) vs Correct Implementation");
        System.out.println("=" + "=".repeat(59));
        
        // Test with various datasets
        testDataset("Simple: [1, 2, 3, 4, 5]", new double[]{1, 2, 3, 4, 5});
        testDataset("Two values: [1, 5]", new double[]{1, 5});
        testDataset("M/M/1 response times", new double[]{0.1, 0.5, 1.2, 0.3, 2.1, 0.7, 0.9, 1.4, 0.6, 0.2, 0.8, 1.1, 0.4, 1.8, 0.9});
        
        // Test the specific scenario from the problem statement
        System.out.println("\n" + "=".repeat(60));
        System.out.println("M/M/1 Queue Theoretical Test");
        System.out.println("λ = 1/0.2 = 5 arrivals/sec, μ = 1/0.16 = 6.25 services/sec");
        System.out.println("Expected mean response time = 1/(μ-λ) = 1/1.25 = 0.8 seconds");
        
        // Generate some exponentially distributed response times with mean 0.8
        double[] theoreticalResponseTimes = generateExponentialSamples(0.8, 1000, 12345);
        double theoreticalMean = 0;
        for (double rt : theoreticalResponseTimes) {
            theoreticalMean += rt;
        }
        theoreticalMean /= theoreticalResponseTimes.length;
        
        System.out.printf("Generated %d samples with empirical mean: %.6f\n", 
                         theoreticalResponseTimes.length, theoreticalMean);
        
        testDataset("M/M/1 Exponential Samples", theoreticalResponseTimes);
    }
    
    private static void testDataset(String name, double[] data) {
        System.out.println("\nTesting: " + name);
        System.out.println("-".repeat(40));
        
        WelfordSimple buggyWelford = new WelfordSimple();
        CorrectWelford correctWelford = new CorrectWelford();
        
        for (double value : data) {
            buggyWelford.iteration(value);
            correctWelford.update(value);
        }
        
        // Calculate true statistics for verification
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        double trueMean = sum / data.length;
        
        double sumSqDiff = 0;
        for (double value : data) {
            double diff = value - trueMean;
            sumSqDiff += diff * diff;
        }
        double truePopVar = sumSqDiff / data.length;
        double trueSampleVar = data.length > 1 ? sumSqDiff / (data.length - 1) : 0;
        
        System.out.printf("True mean: %.6f\n", trueMean);
        System.out.printf("True population variance: %.6f\n", truePopVar);
        System.out.printf("True sample variance: %.6f\n", trueSampleVar);
        System.out.println();
        
        System.out.printf("Buggy Welford - Mean: %.6f, Variance: %.6f\n", 
                         buggyWelford.getAvg(), buggyWelford.getVariance());
        System.out.printf("Correct Welford - Mean: %.6f, Pop Var: %.6f, Sample Var: %.6f\n", 
                         correctWelford.getMean(), correctWelford.getPopulationVariance(), correctWelford.getSampleVariance());
        
        // Check for bugs
        double meanError = Math.abs(buggyWelford.getAvg() - trueMean);
        double varErrorPop = Math.abs(buggyWelford.getVariance() - truePopVar);
        double varErrorSample = Math.abs(buggyWelford.getVariance() - trueSampleVar);
        
        System.out.printf("Mean error: %.2e\n", meanError);
        System.out.printf("Variance error (vs population): %.2e\n", varErrorPop);
        System.out.printf("Variance error (vs sample): %.2e\n", varErrorSample);
        
        if (meanError > 1e-10) {
            System.out.println("*** BUG DETECTED: Mean calculation is incorrect! ***");
        }
        if (varErrorPop > 1e-10 && varErrorSample > 1e-10) {
            System.out.println("*** BUG DETECTED: Variance calculation is incorrect! ***");
        }
    }
    
    // Simple linear congruential generator for reproducible exponential samples
    private static double[] generateExponentialSamples(double mean, int count, long seed) {
        double[] samples = new double[count];
        long state = seed;
        
        for (int i = 0; i < count; i++) {
            // Simple LCG
            state = (state * 1103515245L + 12345L) & 0x7fffffffL;
            double u = state / (double) 0x80000000L;  // uniform [0,1)
            
            // Inverse transform for exponential
            samples[i] = -mean * Math.log(1.0 - u);
        }
        
        return samples;
    }
}