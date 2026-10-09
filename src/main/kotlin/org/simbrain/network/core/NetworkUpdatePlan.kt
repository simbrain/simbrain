/**
 * The cached iteration order for a buffered network update ([Network.bufferedUpdate]). [NetworkModelList] builds a
 * plan from its models in [updatingOrder] and drops it whenever models are added or removed, so a running network does
 * not rebuild or re-sort its model list on every update.
 *
 * Free neurons, the bulk of a large network, are split into chunks that update in parallel (see `ParallelUpdate.kt`):
 * accumulating a neuron's input writes only to that neuron and its own incoming synapses, and updating it writes only
 * to the neuron, unless its rule says otherwise ([NeuronUpdateRule.isNeuronLocal]), in which case neurons update
 * serially. Everything else (arrays, collections, connectors, subnetworks) runs serially in [updatingOrder] after
 * the neurons of each phase. Learning synapses update last, in parallel, so learning rules always see the activations
 * of the current update regardless of model order.
 *
 * The plan also skips work that is known to be empty: synapses have no input accumulation of their own, and synapses
 * with a static learning rule have nothing to update. Which synapses learn is tracked against
 * [Synapse.learningRuleEpoch], since learning rules can change without the model list changing.
 */
package org.simbrain.network.core

import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.updaterules.NeuronUpdateRule

class NetworkUpdatePlan(ordered: List<NetworkModel>) {

    private val neurons: List<Neuron> = ordered.filterIsInstance<Neuron>()

    /**
     * Free neurons in chunks of similar fan-in size, for the input and update phases.
     */
    val neuronChunks: List<List<Neuron>> = chunkByCost(neurons) { 1 + it.fanIn.size }

    /**
     * Chunks for the neuron update phase: [neuronChunks], or a single chunk when some neuron's rule is not
     * [NeuronUpdateRule.isNeuronLocal]. Checked on every update since rules and their settings can change without
     * the model list changing.
     */
    val neuronUpdateChunks: List<List<Neuron>>
        get() = if (neuronChunks.size > 1 && neurons.any { !it.updateRule.isNeuronLocal }) listOf(neurons) else neuronChunks

    /**
     * Models other than free neurons and synapses, in [updatingOrder]. Their inputs are accumulated and they are
     * updated after the neurons of each phase.
     */
    val otherModels: List<NetworkModel> = ordered.filter { it !is Neuron && it !is Synapse }

    private val synapses: List<Synapse> = ordered.filterIsInstance<Synapse>()

    private class LearningSynapses(val epoch: Long, val chunks: List<List<Synapse>>)

    @Volatile
    private var learningSynapses: LearningSynapses? = null

    /**
     * Free synapses with a non-static learning rule, in chunks.
     */
    val learningSynapseChunks: List<List<Synapse>>
        get() {
            val epoch = Synapse.learningRuleEpoch.get()
            learningSynapses?.let { if (it.epoch == epoch) return it.chunks }
            val learning = synapses.filter { it.learningRule !is StaticSynapseRule }
            return chunkByCost(learning) { 1 }.also { learningSynapses = LearningSynapses(epoch, it) }
        }
}
