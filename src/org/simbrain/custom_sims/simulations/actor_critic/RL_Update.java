package org.simbrain.custom_sims.simulations.actor_critic;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import java.util.Collections;

/**
 * A custom updater for use in applying TD Learning and other custom update
 * features (e.g. only activating one vehicle network at a time based on the
 * output of a feed-forward net).
 * <p>
 * For background on TD Learning see.
 * http://www.scholarpedia.org/article/Temporal_difference_learning
 */
// CHECKSTYLE:OFF
public class RL_Update implements NetworkUpdateAction {

    /**
     * Reference to RL_Sim object that has all the main variables used.
     */
    ActorCritic sim;

    /**
     * Reference to main neurons used in td learning.
     */
    Neuron reward, value, tdError;

    /**
     * This variable is a hack needed because the reward neuron's lastactivation
     * value is not being updated properly in this simulation now.
     * <p>
     * Todo: Remove after fixing the issue. The issue is probably based on
     * coupling update.
     */
    double lastReward;

    /**
     * Current winning output neuron.
     */
    Neuron winner;

    /**
     * Construct the updater.
     */
    public RL_Update(ActorCritic sim) {
        super();
        this.sim = sim;
        reward = sim.reward;
        value = sim.value;
        tdError = sim.tdError;
    }

    @Override
    public String getDescription() {
        return "Custom TD Rule";
    }

    @Override
    public String getLongDescription() {
        return "Custom TD Rule";
    }

    /**
     * Custom update of the network, including application of TD Rules.
     */
    @Override
    public void invoke() {

        // Update neurons and networks
        Network.updateNeurons(Collections.singletonList(sim.value));
        Network.updateNeurons(Collections.singletonList(sim.reward));
        sim.outputs.update();

        // System.out.println("td error:" + value.getActivation() + " + " +
        // reward.getActivation() + " - " + value.getLastActivation());
        sim.tdError.forceSetActivation((reward.getActivation() + sim.gamma * value.getActivation()) - value.getLastActivation());

        // Update all value synapses
        for (Synapse synapse : value.getFanIn()) {
            Neuron sourceNeuron = synapse.getSource();
            // Reinforce based on the source neuron's last activation (not its
            // current value),
            // since that is what the current td error reflects.
            double newStrength = synapse.getStrength() + sim.alpha * tdError.getActivation() * sourceNeuron.getLastActivation();
            // synapse.setStrength(synapse.clip(newStrength));
            synapse.forceSetStrength(newStrength);
            // System.out.println("Value Neuron / Tile neuron (" +
            // sourceNeuron.getId() + "):" + newStrength);
        }

        // Update all actor neurons. Reinforce input > output connection that
        // were active at the last time-step.
        for (Neuron neuron : sim.outputs.getNeuronList()) {
            // Just update the last winner
            if (neuron.getLastActivation() > 0) {
                for (Synapse synapse : neuron.getFanIn()) {
                    Neuron sourceNeuron = synapse.getSource();
                    if (sourceNeuron.getLastActivation() > 0) {
                        double newStrength = synapse.getStrength() + sim.alpha * tdError.getActivation() * sourceNeuron.getLastActivation();
                        // synapse.setStrength(synapse.clip(newStrength));
                        synapse.forceSetStrength(newStrength);
                        // System.out.println(tdError.getActivation() + "," +
                        // sourceNeuron.getLastActivation());                        
                    }
                }

            }
        }

    }

}
