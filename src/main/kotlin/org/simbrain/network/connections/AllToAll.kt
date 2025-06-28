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
        label = "Self-Connections Allowed ",
        description = "Can there exist synapses whose source and target are the same?",
        order = 1
    )
    var allowSelfConnection: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed) {

    override val name: String = "All to All"

    override fun toString(): String {
        return name
    }

    override fun copy(): AllToAll {
        return AllToAll(allowSelfConnection).also {
            commonCopy(it)
        }
    }

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createAllToAllSynapses(source, target, allowSelfConnection)
        polarizeSynapses(syns, percentExcitatory, random)
        return syns
    }

}

/**
 * Connects every source neuron to every target neuron.
 */
fun createAllToAllSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    allowSelfConnection: Boolean = false
): List<Synapse> {
    return (sourceNeurons cartesianProduct targetNeurons)
        .filter { (src, tar) ->
            allowSelfConnection || src !== tar
        }.map { (src, tar) ->
            Synapse(src, tar).apply { strength = 1.0 }
        }
}
