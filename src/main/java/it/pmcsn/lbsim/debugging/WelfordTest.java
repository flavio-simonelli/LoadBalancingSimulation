package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.WelfordSimple;

public class WelfordTest {
    public static void main(String[] args) {
        testWelfordWithKnownData();
    }
    
    public static void testWelfordWithKnownData() {
        System.out.println("Testing Welford algorithm with known data...");
        
        // Test with simple known dataset: [1.0, 2.0, 3.0, 4.0, 5.0]
        // Expected mean = 3.0
        // Expected sample variance = 2.5
        // Expected population variance = 2.0
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        WelfordSimple welford = new WelfordSimple();
        for (double value : data) {
            welford.iteration(value);
        }
        
        System.out.println("Data: [1.0, 2.0, 3.0, 4.0, 5.0]");
        System.out.println("Expected mean: 3.0");
        System.out.println("Expected sample variance: 2.5"); 
        System.out.println("Expected population variance: 2.0");
        System.out.println();
        System.out.println("Current Welford results:");
        System.out.println("Count: " + welford.getI());
        System.out.println("Mean: " + welford.getAvg());
        System.out.println("Variance: " + welford.getVariance());
        System.out.println("Standard deviation: " + welford.getStandardVariation());
        
        // Manual calculation for comparison
        double sum = 0;
        for (double value : data) {
            sum += value;
        }
        double mean = sum / data.length;
        
        double sumOfSquaredDiffs = 0;
        for (double value : data) {
            sumOfSquaredDiffs += (value - mean) * (value - mean);
        }
        double sampleVariance = sumOfSquaredDiffs / (data.length - 1);
        double populationVariance = sumOfSquaredDiffs / data.length;
        
        System.out.println();
        System.out.println("Manual calculation for verification:");
        System.out.println("Mean: " + mean);
        System.out.println("Sample variance: " + sampleVariance);
        System.out.println("Population variance: " + populationVariance);
        
        // Check if results match
        boolean meanMatches = Math.abs(welford.getAvg() - mean) < 1e-10;
        boolean varianceMatches = Math.abs(welford.getVariance() - sampleVariance) < 1e-10 || 
                                Math.abs(welford.getVariance() - populationVariance) < 1e-10;
        
        System.out.println();
        System.out.println("Results match expected values:");
        System.out.println("Mean matches: " + meanMatches);
        System.out.println("Variance matches: " + varianceMatches);
        
        if (!meanMatches || !varianceMatches) {
            System.out.println("\n*** BUG DETECTED IN WELFORD IMPLEMENTATION ***");
        } else {
            System.out.println("\n*** Welford implementation appears correct ***");
        }
    }
}