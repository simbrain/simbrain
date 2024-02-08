package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.getWinner
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * Extends competitive group with functions specific to the hippocampus
 * simulation.
 */
class AlvarezSquire(
    /**
     * Reference to parent simulation.
     */
    private val hippo: Hippocampus, numNeurons: Int
) : CompetitiveGroup(numNeurons) {
    /**
     * Noise generator.
     */
    private var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Construct the group.
     *
     * @param hippo      reference to parent sim
     * @param numNeurons number neurons
     */
    init {
        noiseGenerator = UniformRealDistribution(-.05, .05)
    }

    context(Network)
    override fun update() {
        super.update()

        // For this simulation we can assume that if one neuron is clamped, they
        // all are
        val clamped = neuronList[0].clamped
        val winner = getWinner(neuronList, clamped)

        // Update weights on winning neuron
        for (i in neuronList.indices) {
            val neuron = neuronList[i]
            if (neuron == winner) {
                // TODO: Allow user to choose update function
                // neuron.setActivation(this.getWinValue());
                alvarezSquireUpdate(neuron)
                updateWeights(neuron)
            } else {
                // neuron.setActivation(this.getLoseValue());
            }
        }
        decaySynapses()
    }

    /**
     * Simple decay with random noise.
     */
    private fun alvarezSquireUpdate(neuron: Neuron) {
        // TODO: Use library for clipping
        val `val` = .7 * neuron.activation + neuron.weightedInputs + noiseGenerator.sampleDouble()
        neuron.forceSetActivation(if ((`val` > 0)) `val` else 0.0)
        neuron.forceSetActivation(if ((`val` < 1)) `val` else 1.0)
    }

    /**
     * Decay attached synapses in accordance with Alvarez and Squire 1994.
     */
    private fun decaySynapses() {
        var rho: Double
        for (n in neuronList) {
            for (synapse in n.fanIn) {
                // if (synapse.getSource().getParentGroup() == hippo.hippocampus) {
                //     rho = .04;
                // } else if (synapse.getTarget().getParentGroup() == hippo.hippocampus) {
                //     rho = .04;
                // } else {
                //     rho = .0008;
                // }
                // synapse.decay(rho);
            }
        }
    }

    /**
     * Custom weight update.
     *
     * @param neuron winning neuron whose incoming synapses will be updated
     */
    private fun updateWeights(neuron: Neuron) {
        var lambda: Double

        for (synapse in neuron.fanIn) {
            // if (synapse.getSource().getParentGroup() == hippo.hippocampus) {
            //     lambda = .1;
            // } else if (synapse.getTarget().getParentGroup() == hippo.hippocampus) {
            //     lambda = .1;
            // } else {
            //     lambda = .002;
            // }
            // double deltaw = lambda * synapse.getTarget().getActivation() * (synapse.getSource().getActivation() - synapse.getTarget().getAverageInput());
            // synapse.setStrength(synapse.clip(synapse.getStrength() + deltaw));
        }
    }
}
