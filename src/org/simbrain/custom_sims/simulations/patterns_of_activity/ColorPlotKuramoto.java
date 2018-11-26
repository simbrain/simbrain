package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.simulations.agent_trails.AgentTrails;
import org.simbrain.util.projection.Halo;
import org.simbrain.workspace.updater.UpdateAction;

/**
 * Creates a halo around the predicted next point at each iteration, to make
 * clear what points are being predicted.
 */
public class ColorPlotKuramoto implements UpdateAction {

    /**
     * Reference to simulation object that has all the main variables used.
     */
    KuramotoOscillators sim;

    /**
     * Construct the updater.
     */
    public ColorPlotKuramoto(KuramotoOscillators sim) {
        super();
        this.sim = sim;
    }

    @Override
    public String getDescription() {
        return "Color projection points";
    }

    @Override
    public String getLongDescription() {
        return "Color projection points";
    }

    @Override
    public void invoke() {
        double[] predictedState = sim.predictionRes.getActivations();
        Halo.makeHalo(sim.plot.getProjectionModel().getProjector(), predictedState, (float) sim.errorNeuron.getActivation());
    }

}
