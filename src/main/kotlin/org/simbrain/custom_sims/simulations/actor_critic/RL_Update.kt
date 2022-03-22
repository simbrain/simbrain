package org.simbrain.custom_sims.simulations.actor_critic

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.updateNeurons
import org.simbrain.workspace.updater.UpdateAction

/**
 * A custom updater for use in applying TD Learning and other custom update
 * features (e.g. only activating one vehicle network at a time based on the
 * output of a feed-forward net).
 *
 *
 * For background on TD Learning see.
 * http://www.scholarpedia.org/article/Temporal_difference_learning
 */
// CHECKSTYLE:OFF
class RL_Update(var sim: ActorCritic) : UpdateAction("Custom TD Rule") {
    /**
     * Reference to main neurons used in td learning.
     */
    var reward: Neuron
    var value: Neuron
    var tdError: Neuron

    /**
     * Construct the updater.
     */
    init {
        reward = sim.reward
        value = sim.value
        tdError = sim.tdError
    }

    /**
     * Custom update of the network, including application of TD Rules.
     */
    override suspend operator fun invoke() {

        // Update neurons and networks
        sim.sensorNeurons.update()
        updateNeurons(listOf(sim.value))
        updateNeurons(listOf(sim.reward))
        sim.outputs.update()

        // System.out.println("td error:" + value.getActivation() + " + " +
        // reward.getActivation() + " - " + value.getLastActivation());
        sim.tdError.forceSetActivation(
            (reward.activation
                    + sim.gamma * value.activation)
                    - value.lastActivation
        )

        // Update all value synapses
        for (synapse in value.fanIn) {
            val sourceNeuron = synapse.source
            // Reinforce based on the source neuron's last activation (not its
            // current value),
            // since that is what the current td error reflects.
            val newStrength = synapse.strength + sim.alpha * tdError.activation * sourceNeuron.lastActivation
            synapse.strength = synapse.clip(newStrength)
            //synapse.forceSetStrength(newStrength);
            // System.out.println("Value Neuron / Tile neuron (" +
            // sourceNeuron.getId() + "):" + newStrength);
        }

        // Update all actor neurons. Reinforce input > output connection that
        // were active at the last time-step.
        for (neuron in sim.outputs.neuronList) {
            // Just update the last winner
            if (neuron.lastActivation > 0) {
                for (synapse in neuron.fanIn) {
                    val sourceNeuron = synapse.source
                    if (sourceNeuron.lastActivation > 0) {
                        val newStrength =
                            synapse.strength + sim.alpha * tdError.activation * sourceNeuron.lastActivation
                        synapse.strength = synapse.clip(newStrength)
                        //synapse.forceSetStrength(newStrength);
                        // System.out.println(tdError.getActivation() + "," +
                        // sourceNeuron.getLastActivation());
                    }
                }
            }
        }
    }
}