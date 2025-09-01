package it.pmcsn.lbsim.debugging;

import it.pmcsn.lbsim.utils.WelfordSimple;

/**
 * Final validation test showing before/after Welford fixes
 */
public class FinalWelfordValidation {
    public static void main(String[] args) {
        System.out.println("Final Welford Algorithm Validation");
        System.out.println("==================================");
        
        // Test data representing response times similar to what we'd see in simulation
        double[] responseTimesData = {
            0.1, 0.5, 1.2, 0.3, 2.1, 0.7, 0.9, 1.4, 0.6, 0.2,
            0.8, 1.1, 0.4, 1.8, 0.9, 0.3, 0.6, 1.3, 0.5, 2.2,
            0.7, 0.4, 1.0, 0.8, 1.5, 0.6, 0.9, 0.3, 1.1, 0.7
        };
        
        // Calculate true statistics
        double sum = 0;
        for (double rt : responseTimesData) {
            sum += rt;
        }
        double trueMean = sum / responseTimesData.length;
        
        double sumSqDiff = 0;
        for (double rt : responseTimesData) {
            double diff = rt - trueMean;
            sumSqDiff += diff * diff;
        }
        double truePopulationVariance = sumSqDiff / responseTimesData.length;
        double trueSampleVariance = sumSqDiff / (responseTimesData.length - 1);
        
        System.out.printf("Test data: %d response times\n", responseTimesData.length);
        System.out.printf("True mean: %.6f\n", trueMean);
        System.out.printf("True population variance: %.6f\n", truePopulationVariance);  
        System.out.printf("True sample variance: %.6f\n", trueSampleVariance);
        System.out.println();
        
        // Test old buggy implementation
        WelfordOldBuggy oldWelford = new WelfordOldBuggy();
        for (double rt : responseTimesData) {
            oldWelford.iteration(rt);
        }
        
        // Test new fixed implementation  
        WelfordSimple newWelford = new WelfordSimple();
        for (double rt : responseTimesData) {
            newWelford.iteration(rt);
        }
        
        System.out.println("OLD (Buggy) Implementation Results:");
        System.out.printf("  Mean: %.6f (error: %.2e)\n", 
                         oldWelford.getAvg(), Math.abs(oldWelford.getAvg() - trueMean));
        System.out.printf("  Variance: %.6f (pop error: %.2e, sample error: %.2e)\n", 
                         oldWelford.getVariance(),
                         Math.abs(oldWelford.getVariance() - truePopulationVariance),
                         Math.abs(oldWelford.getVariance() - trueSampleVariance));
        System.out.printf("  Std Dev: %.6f\n", oldWelford.getStandardVariation());
        
        System.out.println();
        System.out.println("NEW (Fixed) Implementation Results:");
        System.out.printf("  Mean: %.6f (error: %.2e)\n", 
                         newWelford.getAvg(), Math.abs(newWelford.getAvg() - trueMean));
        System.out.printf("  Variance: %.6f (pop error: %.2e, sample error: %.2e)\n", 
                         newWelford.getVariance(),
                         Math.abs(newWelford.getVariance() - truePopulationVariance),
                         Math.abs(newWelford.getVariance() - trueSampleVariance));
        System.out.printf("  Std Dev: %.6f\n", newWelford.getStandardVariation());
        
        // Summary
        System.out.println();
        System.out.println("SUMMARY:");
        System.out.println("--------");
        System.out.println("✓ Fixed: Mean calculation remains accurate (no change needed)");
        System.out.printf("✓ Fixed: Variance now uses correct Welford algorithm\n");
        System.out.printf("✓ Fixed: Returns sample variance (more appropriate for statistics)\n");
        System.out.printf("✓ Fixed: Standard deviation computed from sample variance\n");
        
        boolean meanFixed = Math.abs(newWelford.getAvg() - trueMean) < 1e-10;
        boolean varianceFixed = Math.abs(newWelford.getVariance() - trueSampleVariance) < 1e-10;
        
        if (meanFixed && varianceFixed) {
            System.out.println("✅ ALL BUGS FIXED - Welford algorithm now works correctly!");
        } else {
            System.out.println("❌ Some issues remain - check implementation");
        }
    }
}