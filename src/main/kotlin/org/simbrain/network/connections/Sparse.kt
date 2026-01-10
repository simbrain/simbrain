package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.sampleWithoutReplacement
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Connect some percent of possible source-target links. Sparsity or density is between 0 (no connections) and 1 (all
 * to all). Features to allow changing existing sparsity and to hold number of outgoing connections constant are
 * provided.
 *
 * When returning synapse lists methods in this file only return newly added synapses. When sparsity changes
 * from 10 to 20% in a 10 node network, for example, only the 100 new synapses are returned, not all 200 synapses.
 *
 * @author Zoë Tosi
 * @author Yulin Li
 * @author Jeff Yoshimi
 */
class Sparse @JvmOverloads constructor(

    /**
     * What percent (as a probability) of possible connections to make.
     */
    @UserParameter(
        label = "Connection density",
        description = "What percent (as a probability) of possible connections to make.",
        order = 10,
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.01
    )
    var connectionDensity: Double = 0.1,

    /**
     * Whether or not each source neuron is given an equal number of efferent synapses. If true, every source neuron
     * will have exactly the same number of synapses emanating from them, that is, each source will connect to the same
     * number of targets. If you have 10 source neurons and 10 target neurons, and 50% sparsity, then each source neuron
     * will connect to exactly 5 targets.
     */
    @get:JvmName("isEqualizeEfferents")
    @UserParameter(
        label = "Equalize efferents",
        description = "Whether or not each source neuron is given an equal number of efferent synapses.",
        order = 20
    )
    var equalizeEfferents: Boolean = false,

    /**
     *  Whether or not connections where the source and target are the same neuron are allowed.
     *  Only applicable if the source and target neuron sets are the same.
     */
    @get:JvmName("isSelfConnectionAllowed")
    @UserParameter(
        label = "Allow self connection",
        description = "Whether or not connections where the source and target are the same neuron are allowed.",
        order = 30
    )
    var allowSelfConnection: Boolean = false,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed), EditableObject {

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val result = createSparseSynapses(source, target, connectionDensity, allowSelfConnection, equalizeEfferents, random)
        return when(result) {
            is ConnectionsResult.Add -> {
                polarizeSynapses(result.connectionsToAdd, percentExcitatory)
                weightInitializer.initializeWeights(result.connectionsToAdd)
                result.connectionsToAdd
            }
            is ConnectionsResult.Reset -> {
                polarizeSynapses(result.resultConnections, percentExcitatory)
                weightInitializer.initializeWeights(result.resultConnections)
                result.resultConnections
            }
            is ConnectionsResult.Remove -> {
                result.connectionsToRemove.forEach { it.deleteBlocking() }
                listOf()
            }
        }
    }

    override val name = "Sparse"

    override fun toString() = name

    override fun tooltipText(): String = "Sparse (${(connectionDensity * 100).toInt()}%)"

    override fun copy(): Sparse {
        return Sparse(connectionDensity, equalizeEfferents, allowSelfConnection).also {
            commonCopy(it)
        }
    }

}

fun createEqualizedSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    connectionDensity: Double,
    selfConnectionAllowed: Boolean = false,
    random: Random = Random
): ConnectionsResult.Reset {

    if (sourceNeurons.isEmpty() || targetNeurons.isEmpty()) {
        return ConnectionsResult.Reset(listOf())
    }

    val connectionCount = (targetNeurons.size * connectionDensity).toInt()

    val connections = sourceNeurons.flatMap { source ->
        (targetNeurons - if (selfConnectionAllowed) emptySet() else setOf(source))
            .sampleWithoutReplacement(random = random)
            .take(connectionCount)
            .map { target -> Synapse(source, target) }
    }

    return ConnectionsResult.Reset(connections)
}

fun createSparseSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    sparsity: Double,
    selfConnectionAllowed: Boolean = false,
    random: Random = Random
): ConnectionsResult {
    if (sourceNeurons.isEmpty() || targetNeurons.isEmpty()) {
        return ConnectionsResult.Add(listOf())
    }
    val existingSynapses = sourceNeurons.flatMap { it.fanOut.values.filter{ it.target in targetNeurons } }
    val possibleConnections = (sourceNeurons.asSequence() cartesianProduct targetNeurons.asSequence()).toSet().let {
        if (!selfConnectionAllowed) {
            it.filter { (source, target) -> source != target }
        } else {
            it
        }
    }
    val connectionDensity = existingSynapses.size.toDouble() / possibleConnections.size
    val sparsityDelta = sparsity - connectionDensity

    if (sparsityDelta >= 0) {
        val existingConnections = existingSynapses.map { it.source to it.target }.toSet()
        val newConnectionCount = (sparsityDelta * possibleConnections.size).roundToInt()
        val availableConnections = possibleConnections - existingConnections
        val connectionsToAdd = availableConnections
            .shuffled(random)
            .take(newConnectionCount)
            .map { (source, target) -> Synapse(source, target) }
        return ConnectionsResult.Add(connectionsToAdd)
    } else {
        val numbersOfConnectionToRemove = -(sparsityDelta * possibleConnections.size).roundToInt()
        val connectionsToRemove = existingSynapses.shuffled(random).take(numbersOfConnectionToRemove)
        return ConnectionsResult.Remove(connectionsToRemove, numbersOfConnectionToRemove == existingSynapses.size)
    }
}

@JvmOverloads
fun createSparseSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    sparsity: Double = .01,
    selfConnectionAllowed: Boolean = false,
    equalizeEfferents: Boolean = false,
    random: Random = Random
): ConnectionsResult = if (equalizeEfferents) {
    createEqualizedSynapses(sourceNeurons, targetNeurons, sparsity, selfConnectionAllowed, random)
} else {
    createSparseSynapses(sourceNeurons, targetNeurons, sparsity, selfConnectionAllowed, random)
}

sealed interface ConnectionsResult {
    data class Add(val connectionsToAdd: List<Synapse>) : ConnectionsResult
    data class Remove(val connectionsToRemove: List<Synapse>, val removedAll: Boolean): ConnectionsResult
    data class Reset(val resultConnections: List<Synapse>): ConnectionsResult
}