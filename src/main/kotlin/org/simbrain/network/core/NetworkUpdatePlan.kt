/**
 * The cached iteration order for a buffered network update ([Network.bufferedUpdate]). [NetworkModelList] builds a
 * plan from its models in [updatingOrder] and drops it whenever models are added or removed, so a running network does
 * not rebuild or re-sort its model list on every update.
 *
 * Free neurons, the bulk of a large network, are split into chunks that update in parallel (see `ParallelUpdate.kt`):
 * accumulating a neuron's input writes only to that neuron and its own incoming synapses, and updating it writes only
 * to the neuron and its data holder. A rule that depends on other neurons reads them while inputs are accumulated
 * ([NeuronUpdateRule.synapticInput]), when every neuron still holds the previous update's state, so the result does
 * not depend on update order. Neurons update serially only when several of them share one rule object that adds
 * seeded noise: they would draw from one generator in thread order, so the run would not repeat for that seed. Everything else (arrays,
 * collections, connectors, subnetworks) runs serially in [updatingOrder] after the neurons of each phase. Learning
 * synapses update last, in parallel, so learning rules always see the activations of the current update regardless
 * of model order.
 *
 * The plan also skips work that is known to be empty: synapses have no input accumulation of their own, and synapses
 * with a static learning rule have nothing to update. Which rules are shared and which synapses learn can change
 * without the model list changing, so [Network] also drops the plan when a neuron's update rule or a synapse's
 * learning rule is replaced.
 */
package org.simbrain.network.core

import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule
import java.util.*

class NetworkUpdatePlan(ordered: List<NetworkModel>) {

    private val neurons: List<Neuron> = ordered.filterIsInstance<Neuron>()

    /**
     * Free neurons in chunks of similar fan-in size, for the input and update phases.
     */
    val neuronChunks: List<List<Neuron>> = chunkByCost(neurons) { 1 + it.fanIn.size }

    /**
     * Chunks for the neuron update phase: [neuronChunks], or a single chunk when a shared rule adds seeded noise.
     * Checked on every update since noise settings can change without the model list or any rule changing.
     */
    val neuronUpdateChunks: List<List<Neuron>>
        get() = if (neuronChunks.size > 1 && sharedUpdateRules.any { it.addsSeededNoise }) listOf(neurons)
        else neuronChunks

    /**
     * Rule objects used by more than one neuron.
     */
    private val sharedUpdateRules: List<NeuronUpdateRule<*, *>> = IdentityHashMap<NeuronUpdateRule<*, *>, Int>()
        .also { counts -> neurons.forEach { counts.merge(it.updateRule, 1, Int::plus) } }
        .filterValues { it > 1 }.keys.toList()

    private val NeuronUpdateRule<*, *>.addsSeededNoise: Boolean
        get() = this is NoisyUpdateRule && addNoise && noiseGenerator.randomSeed != null

    /**
     * Models other than free neurons and synapses, in [updatingOrder]. Their inputs are accumulated and they are
     * updated after the neurons of each phase.
     */
    val otherModels: List<NetworkModel> = ordered.filter { it !is Neuron && it !is Synapse }

    /**
     * Free synapses with a non-static learning rule, in chunks.
     */
    val learningSynapseChunks: List<List<Synapse>> =
        chunkByCost(ordered.filterIsInstance<Synapse>().filter { it.isLearning }) { 1 }
}
