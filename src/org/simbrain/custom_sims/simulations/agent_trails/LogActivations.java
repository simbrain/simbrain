package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.util.Utils;

/**
 * TODO:
 */
public class LogActivations implements NetworkUpdateAction {

    /** Reference to simulation object that has all the main variables used. */
    AgentTrails sim;

    /**
     * Construct the updater.
     */
    public LogActivations(AgentTrails sim) {
        super();
        this.sim = sim;
    }

    public void invoke() {
        sim.activationList.add(
                Utils.getVectorString(sim.sensoryNet.getActivations(), ",", 4)
                        + "," + Utils.getVectorString(
                                sim.predictionNet.getActivations(), ",", 4));
    }

    // This is how the action appears in the update manager dialog
    public String getDescription() {
        return "Log activations";
    }

    // This is a longer description for the tooltip
    public String getLongDescription() {
        return "Log activations, which are saved to a csv file for analysis in other programs";
    }

}
