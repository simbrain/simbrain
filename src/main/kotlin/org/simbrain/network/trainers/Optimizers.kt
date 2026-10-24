package org.simbrain.network.trainers

import org.simbrain.util.UserParameter
import org.simbrain.util.alignedColumnMajorArrays
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

    /**
     * Called once at the start of each optimizer step, before [computeDelta] is called for each parameter matrix.
     */
    open fun beginStep() {}

    context(SupervisedTrainer)
    abstract fun computeDelta(matrix: Matrix, delta: Matrix): Matrix

    context(SupervisedTrainer)
    abstract fun reset()

    override fun getTypeList() = listOf(
        BasicOptimizer::class.java,
        AdamOptimizer::class.java,
        AdamWOptimizer::class.java
    )

    abstract override fun copy(): Optimizer
}

class BasicOptimizer(
    @UserParameter(
        label = "Momentum",
        description = "Weight applied to previous gradient update. Higher values (0.8-0.9) accelerate learning in consistent directions. 0 disables momentum",
        minimumValue = 0.0,
        maximumValue = 1.0,
        order = 1
    )
    var momentum: Double = 0.9
) : Optimizer() {
    private var matrixToVelocityMap: HashMap<Matrix, Matrix> = HashMap()

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val velocity = matrixToVelocityMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        velocity.mul(momentum).add(delta)
        return velocity.clone().mul(learningRate)
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixToVelocityMap.clear()
    }

    override fun copy() = BasicOptimizer(momentum).also { it.learningRate = learningRate }
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

    /**
     * Optimizer steps since the last reset, which bias correction counts. Trainer iterations are not steps: an epoch
     * or sequential update takes many steps in one iteration.
     */
    private var stepsSinceReset = 0

    private val timeSinceLastReset get() = stepsSinceReset.coerceAtLeast(1)

    override fun beginStep() {
        stepsSinceReset++
    }

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        return adamStep(matrix, delta, meanEstimate, varianceEstimate, beta1, beta2, timeSinceLastReset, learningRate, 0.0)
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        stepsSinceReset = 0
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

    /**
     * Optimizer steps since the last reset, which bias correction counts. Trainer iterations are not steps: an epoch
     * or sequential update takes many steps in one iteration.
     */
    private var stepsSinceReset = 0

    private val timeSinceLastReset get() = stepsSinceReset.coerceAtLeast(1)

    override fun beginStep() {
        stepsSinceReset++
    }

    context(SupervisedTrainer)
    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }

        // Apply learning rate decay
        val currentLearningRate = if (learningRateDecay > 0.0) {
            learningRate * (1.0 - learningRateDecay).pow(timeSinceLastReset)
        } else {
            learningRate
        }

        // AdamW: the moments use the original gradients (no weight decay mixed in), and decay is applied directly to
        // the weights (decoupled)
        return adamStep(
            matrix, delta, meanEstimate, varianceEstimate, beta1, beta2, timeSinceLastReset, currentLearningRate,
            weightDecay
        )
    }

    context(SupervisedTrainer)
    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        stepsSinceReset = 0
    }

    override fun copy() = AdamWOptimizer(beta1, beta2, weightDecay, learningRateDecay).also { it.learningRate = learningRate }
}

/**
 * One Adam step for one parameter matrix, in a single pass per element: updates the running [mean] and [variance] in
 * place and returns the update, lr (m / c1) / (sqrt(v / c2) + eps), minus lr * [weightDecay] * w when [weightDecay] is
 * positive (AdamW). Updates are added to the weights, so decay toward zero is subtracted. The operations are the
 * same, in the same order, as the matrix formulation, so the results are identical.
 */
private fun adamStep(
    weights: Matrix,
    delta: Matrix,
    mean: Matrix,
    variance: Matrix,
    beta1: Double,
    beta2: Double,
    step: Int,
    learningRate: Double,
    weightDecay: Double
): Matrix {
    val meanCorrection = 1 - beta1.pow(step)
    val varianceCorrection = 1 - beta2.pow(step)
    val decayRate = learningRate * weightDecay
    val decay = weightDecay > 0.0
    val update = Matrix(weights.nrow(), weights.ncol())
    val arrays = alignedColumnMajorArrays(delta, mean, variance, update, weights)
    if (arrays != null) {
        // Over the backing arrays, so the JIT can vectorize the loop
        val (g, means, variances, out, w) = arrays
        val ld = update.ld()
        for (j in 0 until weights.ncol()) {
            val start = j * ld
            for (k in start until start + weights.nrow()) {
                val m = means[k] * beta1 + g[k] * (1 - beta1)
                val v = variances[k] * beta2 + (g[k] * g[k]) * (1 - beta2)
                means[k] = m
                variances[k] = v
                val adam = (m / meanCorrection) * learningRate / (sqrt(v / varianceCorrection) + 1e-8)
                out[k] = if (decay) adam - w[k] * decayRate else adam
            }
        }
        return update
    }
    for (j in 0 until weights.ncol()) {
        for (i in 0 until weights.nrow()) {
            val g = delta[i, j]
            val m = mean[i, j] * beta1 + g * (1 - beta1)
            val v = variance[i, j] * beta2 + (g * g) * (1 - beta2)
            mean[i, j] = m
            variance[i, j] = v
            val adam = (m / meanCorrection) * learningRate / (sqrt(v / varianceCorrection) + 1e-8)
            update[i, j] = if (decay) adam - weights[i, j] * decayRate else adam
        }
    }
    return update
}
