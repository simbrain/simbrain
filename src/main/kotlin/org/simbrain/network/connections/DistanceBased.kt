package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getEuclideanDist
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.random.Random

class DistanceBased (

    /**
     * Amount to decay connection probabilty as a function of pixel distance
     */
    @UserParameter(
        label = "Distance function",
        description = "Decay function for connection probability.",
        order = 1)
    var decayFunction: DecayFunction = GaussianDecayFunction(),

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createRadialSynapses(source, target, decayFunction, random)
        polarizeSynapses(syns, percentExcitatory, random)
        weightInitializer.initializeWeights(syns)
        return syns
    }

    override fun toString(): String {
        return name
    }

    override fun copy(): DistanceBased {
        return DistanceBased(decayFunction).also {
            commonCopy(it)
        }
    }

    override val name = "Distance Based"

}

fun createRadialSynapses (
    source: List<Neuron>,
    target: List<Neuron>,
    decay: DecayFunction,
    random: Random = Random
): List<Synapse> {
    val syns = ArrayList<Synapse>()
    (source cartesianProduct target).forEach{ (src, tar) ->
        if (src != tar) {
            val p = decay.getScalingFactor(getEuclideanDist(src, tar))
            if (random.nextDouble() < p) {
                syns.add(Synapse(src, tar))
            }
        }
    }
    return syns
}
