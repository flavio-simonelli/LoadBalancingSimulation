import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;
import it.pmcsn.lbsim.utils.random.Rvgs;

public class HyperExponentialTest {
    
    public static void main(String[] args) {
        System.out.println("=== HyperExponential Distribution Test ===");
        
        // Test parameters: mean=0.15, CV=4 (as specified in problem)
        double mean = 0.15;
        double cv = 4.0;
        
        System.out.println("Testing HyperExponential with mean=" + mean + ", cv=" + cv);
        
        HyperExponential hyperExp = new HyperExponential(cv, mean, 0, 1, 2);
        
        System.out.println("Calculated parameters:");
        System.out.println("  p = " + hyperExp.getP());
        System.out.println("  m1 = " + hyperExp.getM1());
        System.out.println("  m2 = " + hyperExp.getM2());
        
        // Verify the mathematical formulas
        System.out.println("\n=== Verifying mathematical formulas ===");
        
        // For hyperexponential with mean μ and CV c:
        // p = (1 + sqrt(1 - 2/(c²+1))) / 2  (choosing the smaller root)
        // λ1 = 2p/μ, λ2 = 2(1-p)/μ
        // m1 = 1/λ1 = μ/(2p), m2 = 1/λ2 = μ/(2(1-p))
        
        double c2 = cv * cv;
        double expectedP = (1 - Math.sqrt(1 - 2.0/(c2 + 1))) / 2.0;
        double expectedM1 = mean / (2 * expectedP);
        double expectedM2 = mean / (2 * (1 - expectedP));
        
        System.out.println("Expected values:");
        System.out.println("  p = " + expectedP);
        System.out.println("  m1 = " + expectedM1);
        System.out.println("  m2 = " + expectedM2);
        
        // Check if our implementation matches
        System.out.println("Matches:");
        System.out.println("  p matches: " + (Math.abs(hyperExp.getP() - expectedP) < 1e-10));
        System.out.println("  m1 matches: " + (Math.abs(hyperExp.getM1() - expectedM1) < 1e-10));
        System.out.println("  m2 matches: " + (Math.abs(hyperExp.getM2() - expectedM2) < 1e-10));
        
        // Verify that the mean is correct: E[X] = p*m1 + (1-p)*m2 = μ
        double actualMean = hyperExp.getP() * hyperExp.getM1() + (1 - hyperExp.getP()) * hyperExp.getM2();
        System.out.println("\nMean verification:");
        System.out.println("  Theoretical mean: " + mean);
        System.out.println("  Actual mean: " + actualMean);
        System.out.println("  Mean correct: " + (Math.abs(actualMean - mean) < 1e-10));
        
        // Test variance: For hyperexponential, Var[X] = E[X²] - (E[X])²
        // E[X²] = p*(2*m1²) + (1-p)*(2*m2²) for exponential components
        double expectedVariance = mean * mean * (cv * cv);
        double expectedEX2 = hyperExp.getP() * 2 * hyperExp.getM1() * hyperExp.getM1() + 
                            (1 - hyperExp.getP()) * 2 * hyperExp.getM2() * hyperExp.getM2();
        double actualVariance = expectedEX2 - actualMean * actualMean;
        System.out.println("\nVariance verification:");
        System.out.println("  Expected variance: " + expectedVariance);
        System.out.println("  Actual variance: " + actualVariance);
        System.out.println("  Variance correct: " + (Math.abs(actualVariance - expectedVariance) < 1e-10));
    }
}