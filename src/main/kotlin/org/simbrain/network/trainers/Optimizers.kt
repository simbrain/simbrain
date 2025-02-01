package org.simbrain.network.trainers

import org.simbrain.util.UserParameter
import org.simbrain.util.applyFunction
import org.simbrain.util.propertyeditor.CopyableObject
import smile.math.matrix.Matrix
import kotlin.math.pow
import kotlin.math.sqrt

abstract class Optimizer: CopyableObject {

    @UserParameter(label = "Learning Rate", increment = .01, minimumValue = 0.0, order = 1)
    var learningRate = 0.01

    context(SupervisedTrainer<*>)
    abstract fun computeDelta(matrix: Matrix, delta: Matrix): Matrix

    context(SupervisedTrainer<*>)
    abstract fun reset()

    override fun getTypeList() = listOf(
        MomentumOptimizer::class.java,
        AdamOptimizer::class.java
    )
}

class MomentumOptimizer(
    @UserParameter(
        label = "Momentum",
        description = "How much to weight the last delta. 0 turns it off. .8-.9 are standard defaults.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        order = 1
    )
    var momentum: Double = 0.9
) : Optimizer() {
    private var matrixToLastDeltaMap: HashMap<Matrix, Matrix> = HashMap()

    context(SupervisedTrainer<*>)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val lastDelta = matrixToLastDeltaMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val adjustment = lastDelta.mul(momentum)
        matrixToLastDeltaMap[matrix] = delta.clone()
        return adjustment.add(delta).mul(learningRate)
    }

    context(SupervisedTrainer<*>)
    override fun reset() {
        matrixToLastDeltaMap.clear()
    }

    override fun copy() = MomentumOptimizer(momentum).also { it.learningRate = learningRate }
}

class AdamOptimizer(
    @UserParameter(label = "Beta1", minimumValue = 0.0, maximumValue = 1.0, order = 1) var beta1: Double = 0.9,
    @UserParameter(label = "Beta2", minimumValue = 0.0, maximumValue = 1.0, order = 2) var beta2: Double = 0.999
) : Optimizer() {

    private val matrixRunningMeanMap: HashMap<Matrix, Matrix> = HashMap()
    private val matrixRunningVarianceMap: HashMap<Matrix, Matrix> = HashMap()

    private var initialIteration = 0

    context(SupervisedTrainer<*>)
    private val timeSinceLastReset get() = (iteration - initialIteration).coerceAtLeast(1)

    context(SupervisedTrainer<*>)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }

        meanEstimate.mul(beta1).add(delta.clone().mul(1 - beta1))
        varianceEstimate.mul(beta2).add(delta.clone().applyFunction { it * it }.mul(1 - beta2))

        val meanCorrected = meanEstimate.clone().div(1 - beta1.pow(timeSinceLastReset))
        val varianceCorrected = varianceEstimate.clone().div(1 - beta2.pow(timeSinceLastReset))

        return meanCorrected.mul(learningRate).div(varianceCorrected.applyFunction { sqrt(it) + 1e-8 })
    }

    context(SupervisedTrainer<*>)
    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        initialIteration = iteration
    }

    override fun copy() = AdamOptimizer(beta1, beta2).also { it.learningRate = learningRate }
}