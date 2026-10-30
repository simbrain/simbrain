/**
 * Optimizers that turn a parameter matrix's accumulated gradient into the update the trainer adds to it: plain
 * gradient descent with momentum, Adam, and AdamW. The trainer calls [Optimizer.beginStep] once per step, then
 * [Optimizer.computeDelta] for each parameter matrix.
 */
package org.simbrain.network.trainers

import org.simbrain.util.UserParameter
import org.simbrain.util.alignedColumnMajorArrays
import org.simbrain.util.columnMajorCopy
import org.simbrain.util.copyFrom
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

    abstract fun computeDelta(matrix: Matrix, delta: Matrix): Matrix

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

    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val velocity = matrixToVelocityMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        velocity.mul(momentum).add(delta)
        return velocity.clone().mul(learningRate)
    }

    override fun reset() {
        matrixToVelocityMap.clear()
    }

    override fun copy() = BasicOptimizer(momentum).also { it.learningRate = learningRate }
}

/**
 * State and step shared by [AdamOptimizer] and [AdamWOptimizer]: running moments per parameter matrix and the step
 * count that bias correction uses. Subclasses choose the learning rate for a step and the weight decay.
 */
abstract class AdamFamilyOptimizer(
    @UserParameter(
        label = "Beta1", 
        description = "Exponential decay rate for first moment estimates (momentum). Controls how much past gradients influence current update.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 1
    ) var beta1: Double,
    @UserParameter(
        label = "Beta2", 
        description = "Exponential decay rate for second moment estimates (variance). Controls adaptive learning rate scaling.",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 2
    ) var beta2: Double
) : Optimizer() {

    private val matrixRunningMeanMap: HashMap<Matrix, Matrix> = HashMap()
    private val matrixRunningVarianceMap: HashMap<Matrix, Matrix> = HashMap()

    /**
     * Optimizer steps since the last reset, which bias correction counts. Trainer iterations are not steps: an epoch
     * or sequential update takes many steps in one iteration.
     */
    private var stepsSinceReset = 0

    protected val timeSinceLastReset get() = stepsSinceReset.coerceAtLeast(1)

    override fun beginStep() {
        stepsSinceReset++
    }

    /** The learning rate for the current step. */
    protected open val stepLearningRate: Double get() = learningRate

    /** Decay applied directly to the weights, decoupled from the gradients; zero for plain Adam. */
    protected open val decoupledWeightDecay: Double get() = 0.0

    override fun computeDelta(matrix: Matrix, delta: Matrix): Matrix {
        val meanEstimate = matrixRunningMeanMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        val varianceEstimate = matrixRunningVarianceMap.getOrPut(matrix) { Matrix(matrix.nrow(), matrix.ncol()) }
        return adamStep(
            weights = matrix,
            delta = delta,
            mean = meanEstimate,
            variance = varianceEstimate,
            beta1 = beta1,
            beta2 = beta2,
            step = timeSinceLastReset,
            learningRate = stepLearningRate,
            weightDecay = decoupledWeightDecay
        )
    }

    override fun reset() {
        matrixRunningMeanMap.clear()
        matrixRunningVarianceMap.clear()
        stepsSinceReset = 0
    }
}

class AdamOptimizer(beta1: Double = 0.9, beta2: Double = 0.999) : AdamFamilyOptimizer(beta1, beta2) {

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
    beta1: Double = 0.9,
    beta2: Double = 0.999,
    @UserParameter(
        label = "Weight Decay", 
        description = "L2 regularization strength applied directly to weights (decoupled from gradients). Helps prevent overfitting.",
        minimumValue = 0.0, 
        order = 3
    ) var weightDecay: Double = 0.01,
    @UserParameter(
        label = "Learning Rate Decay", 
        description = "Fraction by which the learning rate shrinks at each optimizer step: each batch, or each example " +
                "when training one example at a time. 0.0 means no decay. Higher values decay faster",
        minimumValue = 0.0, 
        maximumValue = 1.0, 
        order = 4
    ) var learningRateDecay: Double = 0.0
) : AdamFamilyOptimizer(beta1, beta2) {

    // Apply learning rate decay
    override val stepLearningRate: Double
        get() = if (learningRateDecay > 0.0) {
            learningRate * (1.0 - learningRateDecay).pow(timeSinceLastReset)
        } else {
            learningRate
        }

    // AdamW: the moments use the original gradients (no weight decay mixed in), and decay is applied directly to
    // the weights (decoupled)
    override val decoupledWeightDecay: Double get() = weightDecay

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
    // Over the backing arrays, so the JIT can vectorize the loop. Matrices not laid out alike, such as a transposed
    // delta, are copied into ones that are, and the moments copied back.
    val aligned = alignedColumnMajorArrays(delta, mean, variance, update, weights) != null
    val g = if (aligned) delta else delta.columnMajorCopy()
    val means = if (aligned) mean else mean.columnMajorCopy()
    val variances = if (aligned) variance else variance.columnMajorCopy()
    val out = update
    val w = if (aligned) weights else weights.columnMajorCopy()
    val (ga, ma, va, oa, wa) = alignedColumnMajorArrays(g, means, variances, out, w)!!
    val ld = out.ld()
    for (j in 0 until weights.ncol()) {
        val start = j * ld
        for (k in start until start + weights.nrow()) {
            val m = ma[k] * beta1 + ga[k] * (1 - beta1)
            val v = va[k] * beta2 + (ga[k] * ga[k]) * (1 - beta2)
            ma[k] = m
            va[k] = v
            val adam = (m / meanCorrection) * learningRate / (sqrt(v / varianceCorrection) + 1e-8)
            oa[k] = if (decay) adam - wa[k] * decayRate else adam
        }
    }
    if (!aligned) {
        mean.copyFrom(means)
        variance.copyFrom(variances)
    }
    return out
}
