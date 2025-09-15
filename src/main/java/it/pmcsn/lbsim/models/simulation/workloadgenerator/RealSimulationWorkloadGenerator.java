package it.pmcsn.lbsim.models.simulation.workloadgenerator;

import it.pmcsn.lbsim.models.simulation.Phase;
import it.pmcsn.lbsim.utils.random.HyperExponential;
import it.pmcsn.lbsim.utils.random.Rngs;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RealSimulationWorkloadGenerator extends DistributionWorkloadGenerator {

    private final List<Phase> phases;
    private int currentPhaseIndex = 0;
    private Logger logger = Logger.getLogger(RealSimulationWorkloadGenerator.class.getName());

    public RealSimulationWorkloadGenerator(
            Rngs rngs,
            HyperExponential interarrival,
            HyperExponential service,
            List<Phase> phases
    ) {
        super(rngs, interarrival, service);
        this.phases = phases;
        this.currentPhaseIndex = 0;
    }

    @Override
    public double nextArrival(double currentTime) {
        // Se siamo entrati in una nuova fase, aggiorna la distribuzione interarrival
        if (currentPhaseIndex + 1 < phases.size()
                && currentTime >= phases.get(currentPhaseIndex + 1).getStartTime()) {
            currentPhaseIndex++;
            Phase newPhase = phases.get(currentPhaseIndex);
            // Ricrea la distribuzione HyperExponential con la nuova mean interarrival
            HyperExponential newInterarrival = new HyperExponential(
                    4.0, // CV fisso a 4.0
                    newPhase.getMeanInterarrival(),
                    super.getInterarrival().getStreamP(),
                    super.getInterarrival().getStreamExp1(),
                    super.getInterarrival().getStreamExp2()
            );
            super.setInterarrival(newInterarrival);
            logger.log(Level.SEVERE, "New interarrival " + newPhase.getMeanInterarrival());

        }
        return super.nextArrival(currentTime);
    }

    public void reset() {
        currentPhaseIndex = 0;
        Phase firstPhase = phases.getFirst();

        HyperExponential resetInterarrival = new HyperExponential(
                4.0, // CV fisso a 4.0
                firstPhase.getMeanInterarrival(),
                super.getInterarrival().getStreamP(),
                super.getInterarrival().getStreamExp1(),
                super.getInterarrival().getStreamExp2()
        );
        super.setInterarrival(resetInterarrival);
    }
}