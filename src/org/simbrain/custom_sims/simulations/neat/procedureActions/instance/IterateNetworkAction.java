package org.simbrain.custom_sims.simulations.neat.procedureActions.instance;

import org.simbrain.custom_sims.simulations.neat.Agent;
import org.simbrain.custom_sims.simulations.neat.procedureActions.InstanceProcedureAction;
import org.simbrain.network.core.Network;

public class IterateNetworkAction implements InstanceProcedureAction {
    private int iterationCount;

    public IterateNetworkAction() {
        this(1);
    }

    public IterateNetworkAction(int iteration) {
        this.setIterationCount(iteration);
    }

    @Override
    public void run(Agent i) {
        Network n = i.getNet();
        for (int j = 0; j < iterationCount; j++) {
            n.bufferedUpdateAllNeurons();
        }
        n.update();
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

}
