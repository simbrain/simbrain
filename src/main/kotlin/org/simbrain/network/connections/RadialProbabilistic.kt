package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.getEuclideanDist
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.random.Random

/**
 * For each neuron, consider every neuron in a radius and make excitatory and inhibitory synapses with them according to
 * some probability.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class RadialProbabilistic(

    // TODO: Implement excitatory vs. inhib probs
    // TODO: Check polarity respected
    // TODO: Distinguish EE, EI, etc?

    /**
     * Probability of making connections to neighboring excitatory neurons. Also used for
     * neurons with no polarity.
     */
    @UserParameter(
        label = "Exc. probability",
        description = "Probability connections will be made to neighbor excitatory (or non-polar) neurons.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 5
    )
    var excitatoryProbability: Double = .8,

    /**
     * Probability of designating a given synapse excitatory. If not, it's
     * inhibitory.
     */
    @UserParameter(
        label = "Inh. probability",
        description = "Probability connections will be made to neighbor inhibitory neurons.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 6
    )
    var inhibitoryProbability: Double = .8,

    /**
     * Radius within which to connect excitatory excNeurons.
     */
    @UserParameter(
        label = "Exc. radius",
        description = "Distance to search for excitatory neurons to connect to.",
        minimumValue = 0.0,
        order = 3
    )
    var excitatoryRadius: Double = 100.0,

    /**
     * Radius within which to connect inhibitory excNeurons.
     */
    @UserParameter(
        label = "Inh. radius",
        description = "Distance to search for inhibitory neurons to connect to.",
        minimumValue = 0.0,
        order = 4
    )
    var inhibitoryRadius: Double = 80.0,

    @UserParameter(
        label = "Allow self connections",
        description = "Allow synapses from neurons to themselves.",
        order = 50
    )
    var allowSelfConnections: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    /**
     * Radial simple sets the polarity implicitly.
     */
    override val usesPolarity: Boolean
        get() = false

    /**
     * Connect neurons.
     */
    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val exc = createSynapsesProbabilistically(
            source, target, excitatoryProbability,
            excitatoryRadius, allowSelfConnections,
            random
        )
        val inh = createSynapsesProbabilistically(
            source, target, inhibitoryProbability,
            inhibitoryRadius, allowSelfConnections,
            random
        )
        val syns = exc + inh
        weightInitializer.initializeWeights(syns)
        return syns
    }


    override val name = "Radial (Probabilistic)"

    override fun toString(): String {
        return name
    }

    override fun tooltipText(): String = "Radial Prob (r=${excitatoryRadius.toInt()})"

    override fun copy(): RadialProbabilistic {
        return RadialProbabilistic(
            excitatoryProbability, inhibitoryProbability, excitatoryRadius, inhibitoryRadius, allowSelfConnections
        ).also {
            commonCopy(it)
        }
    }

}

fun createSynapsesProbabilistically(
    src: List<Neuron>,
    tar: List<Neuron>,
    prob: Double,
    radius: Double,
    allowSelfConnection: Boolean = false,
    random: Random = Random
): List<Synapse> {
    return src.flatMap { n ->
        n.createSynapsesProbabilistically(tar, prob, radius, allowSelfConnection, random)
    }
}

/**
 * Connect a neuron to other neurons in a provided pool, using a provided probability.
 * Synapses are created with default strength (±1.0 based on source polarity).
 * Use a WeightInitializer to set weights after creation.
 */
fun Neuron.createSynapsesProbabilistically(
    pool: List<Neuron>,
    prob: Double,
    radius: Double,
    allowSelfConnection: Boolean = false,
    random: Random = Random
): List<Synapse> {
    return getNeuronsInRadius(pool, radius)
        .filter { otherNeuron ->
            if (!allowSelfConnection) this != otherNeuron else true
        }
        .filter { random.nextDouble() < prob }
        .map { otherNeuron ->
            Synapse(this, otherNeuron)
        }
}


fun Neuron.getNeuronsInRadius(neighbors: List<Neuron>, radius: Double): List<Neuron> {
    val ret = ArrayList<Neuron>()
    for (neuron in neighbors) {
        if (getEuclideanDist(this, neuron) < radius) {
            ret.add(neuron)
        }
    }
    return ret
}

/**
 * Are neurons within a given radius being connected <emp>to</emp> the neuron in
 * question (IN) or are they being connected <emp>from</emp> the neuron in
 * question (OUT)?
 */
enum class Direction(private val description: String) {
    OUT("Outdegree"), IN("Indegree");

    override fun toString(): String {
        return description
    }
}