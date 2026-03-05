package org.simbrain.network.core

import org.simbrain.network.conv.ConvOps
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import kotlin.math.sqrt

/**
 * Connects two [Tensor] nodes via a 2D convolution operation.
 *
 * Kernel weights are stored as a flat array in filter-major order:
 * `[numFilters][inputChannels][kernelSize][kernelSize]`.
 *
 * During [propagate], the convolution result is accumulated into [target]'s inputs.
 */
class ConvolutionConnector(
    source: Tensor, target: Tensor,
    @UserParameter(label = "Kernel Size", description = "Spatial size of convolution kernel", displayOnly = true, order = 1)
    val kernelSize: Int = 3,
    @UserParameter(label = "Num Filters", description = "Number of output filters", displayOnly = true, order = 2)
    val numFilters: Int = 16,
    @UserParameter(label = "Stride", description = "Convolution stride", displayOnly = true, order = 3)
    val stride: Int = 1,
    @UserParameter(label = "Padding", description = "Padding strategy", displayOnly = true, order = 4)
    val padding: Padding = Padding.SAME
) : TensorConnector(source, target) {

    @UserParameter(label = "Kernel Grid", description = "Show all kernels in a grid", tab = "GUI", order = 50)
    var kernelGridMode = false
        set(value) {
            field = value
            events.visualPropertiesChanged.fire()
        }

    private val inputChannels = source.shape.channels
    private val kernelArea = kernelSize * kernelSize

    /** Flat kernel weights: [numFilters][inputChannels][kH][kW] */
    val kernels = DoubleArray(numFilters * inputChannels * kernelArea)

    /** Per-filter biases */
    val filterBiases = DoubleArray(numFilters)

    /** Accumulated kernel gradients for backprop. */
    val kernelGrads = DoubleArray(numFilters * inputChannels * kernelArea)

    /** Accumulated bias gradients for backprop. */
    val biasGrads = DoubleArray(numFilters)

    internal val padH: Int
    internal val padW: Int

    init {
        require(kernelSize > 0) { "kernelSize must be > 0, but was $kernelSize" }
        require(numFilters > 0) { "numFilters must be > 0, but was $numFilters" }
        require(stride > 0) { "stride must be > 0, but was $stride" }

        val (ph, pw) = padding.compute(source.shape.height, source.shape.width, kernelSize, stride)
        padH = ph
        padW = pw

        val expectedShape = source.shape.convOutputShape(kernelSize, stride, padding, numFilters)
        require(target.shape == expectedShape) {
            "Target shape ${target.shape} does not match expected conv output shape $expectedShape"
        }

        // He initialization by default
        heInitialize()
    }

    /**
     * He initialization: weights ~ N(0, sqrt(2 / fan_in)) where fan_in = inputChannels * kernelSize^2
     */
    fun heInitialize() {
        val fanIn = inputChannels * kernelArea
        val std = sqrt(2.0 / fanIn)
        for (i in kernels.indices) {
            kernels[i] = random.nextGaussian() * std
        }
        filterBiases.fill(0.0)
    }

    companion object {
        private val random = java.util.Random()
    }

    override fun propagate() {
        ConvOps.conv2d(
            source.activations, source.shape,
            kernels, numFilters, kernelSize,
            filterBiases,
            target.inputs, target.shape,
            stride, padH, padW
        )
    }

    /**
     * Backward pass: computes kernel/bias gradients and propagates error to source tensor.
     * Assumes target.gradients contains the gradient from downstream.
     *
     * Accumulates into [kernelGrads], [biasGrads], and [source].gradients.
     * Call [clearGrads] and [source].clearGradients() before a new batch.
     */
    fun backward() {
        ConvOps.conv2dBackwardKernels(
            target.gradients, target.shape,
            source.activations, source.shape,
            kernelGrads, numFilters, kernelSize,
            biasGrads,
            stride, padH, padW
        )
        ConvOps.conv2dBackwardInput(
            target.gradients, target.shape,
            kernels, numFilters, kernelSize,
            source.gradients, source.shape,
            stride, padH, padW
        )
    }

    fun clearGrads() {
        kernelGrads.fill(0.0)
        biasGrads.fill(0.0)
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        for (i in kernels.indices) {
            kernels[i] = (randomizer ?: weightRandomizer).sampleDouble()
        }
        events.updated.fire()
    }

    override val name: String get() = "Convolution"

    override fun toString(): String =
        "$displayName (Conv ${kernelSize}x${kernelSize}, ${inputChannels}->$numFilters)"

}
