/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.trainers

import org.simbrain.network.core.*
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.util.*
import smile.math.matrix.Matrix
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.associate
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.filterIsInstance
import kotlin.collections.first
import kotlin.collections.flatMap
import kotlin.collections.flatten
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.getOrPut
import kotlin.collections.intersect
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.reverse
import kotlin.collections.reversed
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMutableSet
import kotlin.collections.toSet
import kotlin.collections.zip

// TODO: Need a way to generalize across NeuronArrays and NeuronCollections
val WeightMatrix.src get() = source as NeuronArray
val WeightMatrix.tar get() = target as NeuronArray

/**
 * Perform a "forward pass" through a list of weight matrices. Assumes they are all connected.
 */
context(Network)
fun List<WeightMatrix>.forwardPass(inputVector: Matrix) {
    inputVector.validateSameShape(first().src.inputs)
    first().src.activations = inputVector

    fun NeuronArray.updateWithoutClearingInputs() {
        updateRule.apply(this, dataHolder)
        events.updated.fire()
    }

    for (wm in this) {
        wm.target.inputs.mul(0.0)
        wm.target.accumulateInputs()
        (wm.target as NeuronArray).updateWithoutClearingInputs()
    }
}


/**
 * Backpropagate the provided errors through this weight matrix, and return the new error.
 */
fun WeightMatrix.updateWeights(layerError: Matrix, epsilon: Double = .1): Matrix {
    layerError.validateSameShape(target.activations)
    val weightDeltas = layerError.mm(source.activations.transpose())

    // Backpropagate the layer error through the weights to get a new error vector
    //  Prefer this to layerError.T.mm(wm).T because that requies an extra transpose
    val backropagatedErrors = weightMatrix.transpose().mm(layerError)

    // Update weights
    weightMatrix.add(weightDeltas.mul(epsilon))
    events.updated.fire()

    return backropagatedErrors
}

fun WeightMatrix.computeDelta(layerError: Matrix): Pair<Matrix, Matrix> {
    layerError.validateSameShape(target.activations)
    val weightDeltas = layerError.mm(source.activations.transpose())

    // Backpropagate the layer error through the weights to get a new error vector
    //  Prefer this to layerError.T.mm(wm).T because that requies an extra transpose
    val backropagatedErrors = weightMatrix.transpose().mm(layerError)

    return Pair(weightDeltas, backropagatedErrors)
}

/**
 * Change to bias is error vector times epsilon. Compute this and add it to biases.
 */
fun NeuronArray.updateBiases(error: Matrix, epsilon: Double = .1) {
    this.activations.validateSameShape(error)
    val biasDelta = error.clone().mul(epsilon)
    biases += biasDelta
    events.updated.fire()
}

enum class BackpropLossFunction {

    SSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual sse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0)
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "SSE"

        override val description = "Sum Squared Error"
    },

    MSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual mse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).mul(2.0).div(actual.size().toDouble())
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "MSE"

        override val description = "Mean Squared Error"

    },

    RMSE {
        override fun scalarLoss(actual: Matrix, target: Matrix) = actual rmse target

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return target.clone().sub(actual).div(actual.size() * scalarLoss(actual, target))
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule !is SoftmaxRule

        override val shortName = "RMSE"

        override val description = "Root Mean Squared Error"

    },

    CrossEntropy {
        override fun scalarLoss(actual: Matrix, target: Matrix) = crossEntropy(actual, target)

        override fun outputError(actual: Matrix, target: Matrix): Matrix {
            actual.validateSameShape(target)
            return actual.clone().sub(target).mul(-1.0) // assume softmax output
        }

        override fun canUse(layer: NeuronArray) = layer.updateRule is SoftmaxRule

        override val shortName = "CrossEntropy"

        override val description = "Cross Entropy"

    };


    abstract fun scalarLoss(actual: Matrix, target: Matrix): Double

    abstract fun outputError(actual: Matrix, target: Matrix): Matrix

    abstract fun canUse(layer: NeuronArray): Boolean

    abstract val shortName: String

    abstract val description: String

    override fun toString() = description

    fun validateLayer(layer: NeuronArray) {
        if (!canUse(layer)) {
            throw IllegalArgumentException("Layer $layer cannot use loss function $this")
        }
    }
}

/**
 * Apply backprop algorithm to this list of matrices, for the provided input/target pair. Assumes weight matrices are
 * stored in a sequence from input to output layers
 */
context(Network)
fun List<WeightMatrix>.applyBackprop(
    targetValues: Matrix,
    epsilon: Double = .1,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE,
    debug: (index: Int, layerError: List<Double>) -> Unit = { _, _ -> }
): Double {

    targetValues.validateSameShape(last().tar.activations)
    lossFunction.validateLayer(last().tar)

    val error = lossFunction.scalarLoss(last().tar.activations, targetValues)

    // printActivationsAndWeights()
    var layerError: Matrix = lossFunction.outputError(last().tar.activations, targetValues)

    this.reversed().forEachIndexed { index, wm ->
        debug(index, layerError.flatten().toList())
        (wm.tar.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.tar.inputs)?.let { deriv ->
            layerError.mul(deriv)
        }
        wm.tar.updateBiases(layerError, epsilon)
        layerError = wm.updateWeights(layerError, epsilon)
    }

    return error
}

context(Network)
fun List<WeightMatrix>.accumulateBackprop(
    targetValues: Matrix,
    weightAccumulator: HashMap<WeightMatrix, Matrix>,
    biasesAccumulator: HashMap<NeuronArray, Matrix>,
    lossFunction: BackpropLossFunction = BackpropLossFunction.SSE
): Double {

    targetValues.validateSameShape(last().tar.activations)
    lossFunction.validateLayer(last().tar)

    val error = lossFunction.scalarLoss(last().tar.activations, targetValues)

    // printActivationsAndWeights()
    var layerError: Matrix = lossFunction.outputError(last().tar.activations, targetValues)

    this.reversed().forEach { wm ->
        (wm.tar.updateRule as? DifferentiableUpdateRule)?.getDerivative(wm.tar.inputs)?.let {
            deriv -> layerError.mul(deriv)
        }
        biasesAccumulator.getOrPut(wm.tar) {
            Matrix(wm.tar.size, 1)
        }.add(layerError)
        val (delta, errors) = wm.computeDelta(layerError)
        layerError = errors
        weightAccumulator.getOrPut(wm) {
            Matrix(wm.weightMatrix.nrow(), wm.weightMatrix.ncol())
        }.add(delta)
    }

    return error
}

context(Network)
fun WeightMatrixTree.forwardPass(inputVectors: List<Matrix>) {
    if (inputVectors.size != inputWeightLayers.size) throw IllegalArgumentException("Must provide same number of input vectors as input layers")
    inputVectors.zip(inputWeightLayers).forEach { (a, b) -> a.validateSameShape(b.src.inputs) }

    inputWeightLayers.zip(inputVectors).forEach { (wm, iv) -> wm.src.activations = iv }
    val allNeuronArrays = LinkedHashSet(tree.flatMap { it.map { it.target } })

    allNeuronArrays.forEach {
        it.accumulateInputs()
        it.update()
    }
}

/**
 * Apply backprop to a tree of weight matrices, beginning with the “output” weight matrix and backpropagating error
 * through incoming weight matrices.
 *
 * Weight matrices are updated one “weight layer” at a time. See [WeightMatrixTree] for more information.
 */
fun WeightMatrixTree.applyBackprop(targetValues: Matrix, lossFunction: BackpropLossFunction = BackpropLossFunction.SSE, epsilon: Double = .0001): Double {

    targetValues.validateSameShape(outputWeightLayer.tar.activations)
    lossFunction.validateLayer(outputWeightLayer.tar)

    val error = lossFunction.scalarLoss(outputWeightLayer.tar.activations, targetValues)
    var errorVectors: Map<NeuronArray, Matrix> =
        mapOf(outputWeightLayer.tar to lossFunction.outputError(outputWeightLayer.tar.activations, targetValues))
    // TODO: Creating a map every iteration is a potential performance drain.
    tree.reversed().forEach { wms ->
        errorVectors = wms.associate { wm ->
            val tar = wm.tar
            val errorVector = errorVectors[tar]!!
            val deriv = (wm.tar.updateRule as DifferentiableUpdateRule).getDerivative(wm.tar.inputs)
            errorVector.mul(deriv)
            wm.tar.updateBiases(errorVector, epsilon)
            wm.src to wm.updateWeights(errorVector, epsilon)
        }

    }
    return error
}

/**
 * Returns a list or chain of connectors from input (start) to output (end).
 */
fun getConnectorChain(start: Layer, end: Layer): List<Connector> {

    // special case for recurrent connections from a layer to itself
    if (start === end) {
        return listOf(start.outgoingConnectors.first { it.target === end })
    }

    fun reconstructPath(start: Layer, end: Layer, path: Map<Layer, Connector>): List<Connector> {
        val result = mutableListOf<Connector>()
        var currentLayer: Layer? = end
        while (currentLayer != null && currentLayer != start) {
            result.add(path[currentLayer]!!)
            currentLayer = path[currentLayer]!!.source
        }

        result.reverse()
        return result
    }

    val visited = mutableSetOf<Layer>()
    val queue = ArrayDeque<Layer>()
    val path = mutableMapOf<Layer, Connector>()

    queue.add(start)

    while (queue.isNotEmpty()) {
        val currentLayer = queue.removeFirst()

        if (currentLayer == end) {
            // We've found the end node, so we'll now reconstruct the path.
            return reconstructPath(start, end, path)
        }

        if (currentLayer in visited) {
            continue
        }

        visited.add(currentLayer)
        for (neighbor in currentLayer.outgoingConnectors) {
            if (neighbor.target !in visited) {
                queue.add(neighbor.target)
                path[neighbor.target] = neighbor
            }
        }
    }

    // If there's no path, return an empty list.
    return emptyList()
}

/**
 * A tree of weight matrices stored in the order they should be updated using backprop. The matrices are organized based
 * on the order in which they are updated during backpropagation. Stored as a list of lists of weight matrices.
 * Example structure:
 * (
 *   (wm_out),                   # Output weight layer
 *   (wm_hidden_1),               # First hidden weight layer
 *   (wm_hidden_21, wm_hidden_22) # Second hidden weight layer with a branch
 *   ...
 * )
 */
class WeightMatrixTree(start: List<Layer>, end: Layer) {
    val tree: List<List<WeightMatrix>>

    init {
        val validLayers = start.flatMap { getConnectorChain(it, end).filterIsInstance<WeightMatrix>() }.toMutableSet()
        tree = sequence {
            var frontier = listOf(end)
            while (validLayers.isNotEmpty()) {
                val weightMatrices = frontier.flatMap { it.incomingConnectors }
                    .filter { validLayers.contains(it) }
                    .filterIsInstance<WeightMatrix>()
                yield(weightMatrices)
                val layers = weightMatrices.map { it.source }
                frontier = layers
                validLayers.removeAll(weightMatrices.toSet())
            }
        }.toList().reversed()
    }

    val inputWeightLayers: List<WeightMatrix> = start
        .map { it.outgoingConnectors }
        .flatten()
        .filterIsInstance<WeightMatrix>()
        .toSet()
        .intersect(tree.flatten().toSet())
        .toList()
    val outputWeightLayer: WeightMatrix = tree.last().first()

}


/**
 * Print debugging info for a list of weight matrices.
 */
context(Network)
fun List<WeightMatrix>.printActivationsAndWeights(showWeights: Boolean = false) {
    println(first().source)
    for (wm in this) {
        wm.target.accumulateInputs()
        wm.target.update()
        println(wm)
        if (showWeights) {
            println(wm.weightMatrix)
        }
        println(wm.target)
    }

}
