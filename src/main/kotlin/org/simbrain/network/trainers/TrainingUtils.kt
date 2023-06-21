
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

import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.util.plusAssign
import org.simbrain.util.sse
import org.simbrain.util.validateSameShape
import smile.math.matrix.Matrix

// TODO: Need a way to generalize across NeuronArrays and NeuronCollections
val WeightMatrix.src get()= source as NeuronArray
val WeightMatrix.tar get()= target as NeuronArray

/**
 * Return the difference between the provided vector and the current activations in this layer.
 */
fun ArrayLayer.getError(targets: Matrix): Matrix {
    outputs.validateSameShape(targets)
    return targets.clone().sub(outputs)
}

/**
 * Apply LMS to the weight matrix using the provided error vector, which must have the same shape as this weight
 * matrix's output
 */
fun WeightMatrix.applyLMS(outputError: Matrix, epsilon: Double = .1) {

    outputError.validateSameShape(target.outputs)

    // TODO: Can this be replaced by backprop with linear, since derivative is then just source activations
    // TODO: Bias
    // TODO: derivative of output activations
    val weightDeltas = outputError.mm(source.outputs.transpose())
    weightMatrix.add(weightDeltas.mul(epsilon))
    events.updated.fireAndForget()
}

/**
 * Learn to produce current target activations (which might have been "force set") from current source activations.
 * Uses least-mean-squares.
 */
fun WeightMatrix.trainCurrentOutputLMS(epsilon: Double = .1) {
    val targets = target.outputs.clone()
    val actualOutputs = output
    applyLMS(targets.sub(actualOutputs), epsilon)
}

// TODO: For hidden layers don't use epsilon. 
/**
 * Backpropagate the provided errors through this weight matrix, and return the new error.
 */
fun WeightMatrix.applyBackprop(layerError: Matrix, epsilon: Double = .1): Matrix {
    layerError.validateSameShape(target.outputs)
    val weightDeltas = layerError.mm(source.outputs.transpose())
    weightMatrix.add(weightDeltas.clone().mul(epsilon))
    events.updated.fireAndBlock()
    return Matrix.column(weightDeltas.mul(weightMatrix).colSums())
}

fun NeuronArray.updateBiases(layerError: Matrix, epsilon: Double = .1) {
    if (dataHolder is BiasedMatrixData) {
        val weightDelta = layerError.clone().mul(epsilon).col(0)
        (dataHolder as BiasedMatrixData).biases += weightDelta
        events.updated.fireAndBlock()
    }
}

/**
 * Print debugging info for a list of weight matrices.
 */
fun List<WeightMatrix>.printActivationsAndWeights(showWeights: Boolean = false) {
    println(first().source)
    for (wm in this) {
        wm.target.updateInputs()
        wm.target.update()
        println(wm)
        if(showWeights) {
            println(wm.weightMatrix)
        }
        println(wm.target)
    }

}

/**
 * Perform a "forward pass" through a list of weight matrices. Assumes they are all connected.
 */
fun List<WeightMatrix>.forwardPass(inputs: Matrix) {
    first().src.activations = inputs
    for (wm in this) {
        wm.target.updateInputs()
        wm.target.update()
    }
}

/**
 * Apply backprop algorithm to this list of matrices, for the provided input/target pair. Assumes weight matrices are
 * stored in a sequence from input to output layers
 */
fun List<WeightMatrix>.applyBackprop(inputVector: Matrix, targetValues: Matrix, epsilon: Double = .1): Double  {

    inputVector.validateSameShape(first().src.inputs)
    targetValues.validateSameShape(last().tar.outputs)

    //TODO: activation function derivatives

    forwardPass(inputVector)
    val error = last().tar.outputs sse targetValues

    // printActivationsAndWeights()
    var errorVector: Matrix = last().tar.getError(targetValues)

    for (wm in this.reversed()) {
        wm.tar.updateBiases(errorVector, epsilon)
        errorVector = wm.applyBackprop(errorVector, epsilon)
    }

    return error

}
