package org.simbrain.network.trainers

import org.simbrain.util.UserParameter
import org.simbrain.util.applyFunction
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.CustomTypeName
import smile.math.matrix.Matrix
import kotlin.math.pow
import kotlin.math.sqrt

abstract class Optimizer: CopyableObject {

    @UserParameter(
        label = "Learning Rate", 
        description = "Step size for gradient descent updates. Higher values learn faster but may overshoot. Typical range: 0.001-0.1",
        increment = .01, 
        minimumValue = 0.0, 
        order = 1
    )
    var learningRate = 0.01

    context(SupervisedTrainer)
    abstract fun computeDelta(matrix: Matrix, delta: Matrix): Matrix

    context(SupervisedTrainer)
    abstract fun reset()

    override fun getTypeList() = listOf(
        MomentumOptimizer::class.java,
        AdamOptimizer::class.java,
        AdamWOptimizer::class.java
    )

    abstract override fun copy(): Optimizer
}

class MomentumOptimizer(
    @UserParameter(
        label = "Momentum",
        description = "Weight applied to previous gradient update. Higher values (0.8-0.9) accelerate learning in consistent directions. 0 disables momentum",
        minimumValue = 0.0,
        maximumValue = 1.0,
        order = 1
    )
    var momentum: Double = 0.9
) : Optimizer() {
    private var matrixToLastDeltaMap: HashMap<Matrix, Matrix> = HashMap()

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val lastDelta = matrixToLastDeltaMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val adjustment = lastDelta.mul(momentum)
        matrixToLastDeltaMap[matrix] = delta.clone()
        return adjustment.add(delta).mul(learningRate)
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixToLastDeltaMap.clear()
    }

    override fun copy() = MomentumOptimizer(momentum).also { it.learningRate = learningRate }
}

class AdamOptimizer(
    @UserParameter(
        label = "Beta1", 
        description = "Exponential decay rate for first moment estimates (momentum). Controls how much past gradients influence current update.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 1
    ) var beta1: Double = 0.9,
    @UserParameter(
        label = "Beta2", 
        description = "Exponential decay rate for second moment estimates (variance). Controls adaptive learning rate scaling.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 2
    ) var beta2: Double = 0.999
) : Optimizer() {

    private val matrixRunningMeanMap: HashMap<Matrix, Matrix> = HashMap()
    private val matrixRunningVarianceMap: HashMap<Matrix, Matrix> = HashMap()

    private var initialIteration = 0

    context(SupervisedTrainer)
    private val timeSinceLastReset get() = (iteration - initialIteration).coerceAtLeast(1)

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }

        meanEstimate.mul(beta1).add(delta.clone().mul(1 - beta1))
        varianceEstimate.mul(beta2).add(delta.clone().applyFunction { it * it }.mul(1 - beta2))

        val meanCorrected = meanEstimate.clone().div(1 - beta1.pow(timeSinceLastReset))
        val varianceCorrected = varianceEstimate.clone().div(1 - beta2.pow(timeSinceLastReset))

        return meanCorrected.mul(learningRate).div(varianceCorrected.applyFunction { sqrt(it) + 1e-8 })
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        initialIteration = iteration
    }

    override fun copy() = AdamOptimizer(beta1, beta2).also { it.learningRate = learningRate }
}


/**
 * AdamW optimizer with decoupled weight decay.
 *
 * AdamW applies weight decay directly to the weights rather than adding it to the gradients,
 * which provides better regularization and is widely used in modern deep learning.
 *
 * Reference: "Decoupled Weight Decay Regularization" by Loshchilov & Hutter (2017)
 */
@CustomTypeName("AdamW Optimizer")
class AdamWOptimizer(
    @UserParameter(
        label = "Beta1", 
        description = "Exponential decay rate for first moment estimates (momentum). Controls how much past gradients influence current update.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 1
    ) var beta1: Double = 0.9,
    @UserParameter(
        label = "Beta2", 
        description = "Exponential decay rate for second moment estimates (variance). Controls adaptive learning rate scaling.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 2
    ) var beta2: Double = 0.999,
    @UserParameter(
        label = "Weight Decay", 
        description = "L2 regularization strength applied directly to weights (decoupled from gradients). Helps prevent overfitting.",
        minimumValue = 0.0, 
        order = 3
    ) var weightDecay: Double = 0.01,
    @UserParameter(
        label = "Learning Rate Decay", 
        description = "Exponential decay rate for learning rate over time. 0.0 means no decay. Higher values decay faster",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 4
    ) var learningRateDecay: Double = 0.0
) : Optimizer() {

    private val matrixRunningMeanMap: HashMap<Matrix, Matrix> = HashMap()
    private val matrixRunningVarianceMap: HashMap<Matrix, Matrix> = HashMap()

    private var initialIteration = 0

    context(SupervisedTrainer)
    private val timeSinceLastReset get() = (iteration - initialIteration).coerceAtLeast(1)

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }

        // AdamW: Use original gradients (no weight decay mixed in)
        meanEstimate.mul(beta1).add(delta.clone().mul(1 - beta1))
        varianceEstimate.mul(beta2).add(delta.clone().applyFunction { it * it }.mul(1 - beta2))

        val meanCorrected = meanEstimate.clone().div(1 - beta1.pow(timeSinceLastReset))
        val varianceCorrected = varianceEstimate.clone().div(1 - beta2.pow(timeSinceLastReset))

        // Apply learning rate decay
        val currentLearningRate = if (learningRateDecay > 0.0) {
            learningRate * (1.0 - learningRateDecay).pow(timeSinceLastReset)
        } else {
            learningRate
        }

        val adamUpdate = meanCorrected.mul(currentLearningRate).div(varianceCorrected.applyFunction { sqrt(it) + 1e-8 })

        // AdamW: Apply weight decay directly to weights (decoupled)
        if (weightDecay > 0.0) {
            val weightDecayUpdate = matrix.clone().mul(currentLearningRate * weightDecay)
            return adamUpdate.add(weightDecayUpdate)
        } else {
            return adamUpdate
        }
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        initialIteration = iteration
    }

    override fun copy() = AdamWOptimizer(beta1, beta2, weightDecay, learningRateDecay).also { it.learningRate = learningRate }
}