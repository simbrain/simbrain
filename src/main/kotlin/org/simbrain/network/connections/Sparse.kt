/*
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.sampleWithoutReplacement
import kotlin.math.roundToInt

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
        label = "Connection Density",
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
        label = "Equalize Efferents",
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
        label = "Allow Self Connection",
        description = "Whether or not connections where the source and target are the same neuron are allowed.",
        order = 30
    )
    var allowSelfConnection: Boolean = NetworkPreferences.selfConnectionAllowed
) : ConnectionStrategy(), EditableObject {

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val result = createSparseSynapses(source, target, connectionDensity, allowSelfConnection, equalizeEfferents)
        return when(result) {
            is ConnectionsResult.Add -> {
                polarizeSynapses(result.connectionsToAdd, percentExcitatory)
                result.connectionsToAdd
            }
            is ConnectionsResult.Reset -> {
                polarizeSynapses(result.resultConnections, percentExcitatory)
                result.resultConnections
            }
            is ConnectionsResult.Remove -> {
                result.connectionsToRemove.forEach { it.delete() }
                listOf()
            }
        }
    }

    override val name = "Sparse"

    override fun toString() = name

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
    random: kotlin.random.Random = kotlin.random.Random(kotlin.random.Random.nextLong())
): ConnectionsResult.Reset {

    if (sourceNeurons.isEmpty() || targetNeurons.isEmpty()) {
        return ConnectionsResult.Reset(listOf())
    }

    val connectionCount = sourceNeurons.size * targetNeurons.size * connectionDensity

    val sources = sourceNeurons.sampleWithoutReplacement(random = random, restartIfExhausted = true)
        .take(connectionCount.roundToInt())

    val targets = targetNeurons.sampleWithoutReplacement(random = random, restartIfExhausted = true)
        .take(connectionCount.roundToInt())

    val connections = (sources zip targets).let {
        if (selfConnectionAllowed) {
            it
        } else {
            it.filter { (source, target) -> source != target }
        }
    }

    return ConnectionsResult.Reset(connections.map { (source, target) -> Synapse(source, target) }.toList())
}

fun createSparseSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    sparsity: Double,
    selfConnectionAllowed: Boolean = false
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
            .shuffled()
            .take(newConnectionCount)
            .map { (source, target) -> Synapse(source, target) }
        return ConnectionsResult.Add(connectionsToAdd)
    } else {
        val numbersOfConnectionToRemove = -(sparsityDelta * possibleConnections.size).roundToInt()
        val connectionsToRemove = existingSynapses.shuffled().take(numbersOfConnectionToRemove)
        return ConnectionsResult.Remove(connectionsToRemove)
    }
}

fun createSparseSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    sparsity: Double = .01,
    selfConnectionAllowed: Boolean = false,
    equalizeEfferents: Boolean = false
): ConnectionsResult = if (equalizeEfferents) {
    createEqualizedSynapses(sourceNeurons, targetNeurons, sparsity, selfConnectionAllowed)
} else {
    createSparseSynapses(sourceNeurons, targetNeurons, sparsity, selfConnectionAllowed)
}

sealed interface ConnectionsResult {
    data class Add(val connectionsToAdd: List<Synapse>) : ConnectionsResult
    data class Remove(val connectionsToRemove: List<Synapse>): ConnectionsResult
    data class Reset(val resultConnections: List<Synapse>): ConnectionsResult
}