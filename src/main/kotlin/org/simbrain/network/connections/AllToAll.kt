package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import kotlin.random.Random

/**
 * Connect every source neuron to every target neuron.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class AllToAll @JvmOverloads constructor(

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    @UserParameter(
        label = "Self-connections allowed",
        description = "Can there exist synapses whose source and target are the same?",
        order = 1
    )
    var allowSelfConnection: Boolean = false,

    /**
     * If true, synapses are added in both directions.
     */
    @UserParameter(label = "Bi-directional", description = "If true, synapses are added in both directions.", order = 2)
    var useBidirectionalConnections: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed) {

    override val name: String = "All to All"

    override fun toString(): String {
        return name
    }

    override fun copy(): AllToAll {
        return AllToAll(allowSelfConnection, useBidirectionalConnections).also {
            commonCopy(it)
        }
    }

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createAllToAllSynapses(source, target, allowSelfConnection, useBidirectionalConnections)
        val polarized = splitSynapsesByPolarity(syns, percentExcitatory, random)
        weightInitializer.initializeWeights(polarized)
        return syns
    }

}

/**
 * Connects every source neuron to every target neuron.
 */
fun createAllToAllSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    allowSelfConnection: Boolean = false,
    useBidirectionalConnections: Boolean = false
): List<Synapse> {
    val synapses = mutableListOf<Synapse>()
    
    // Create synapses from source to target
    (sourceNeurons cartesianProduct targetNeurons)
        .filter { (src, tar) ->
            allowSelfConnection || src !== tar
        }.forEach { (src, tar) ->
            synapses.add(Synapse(src, tar).apply { strength = 1.0 })
        }
    
    // If bidirectional, create synapses from target to source (avoiding duplicates)
    if (useBidirectionalConnections) {
        (targetNeurons cartesianProduct sourceNeurons)
            .filter { (tar, src) ->
                // Only add if it's not the same connection we already added
                (allowSelfConnection || src !== tar) && 
                !synapses.any { it.source === tar && it.target === src }
            }.forEach { (tar, src) ->
                synapses.add(Synapse(tar, src).apply { strength = 1.0 })
            }
    }
    
    return synapses
}
