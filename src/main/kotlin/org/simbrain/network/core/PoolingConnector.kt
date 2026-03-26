package org.simbrain.network.core

import org.simbrain.network.conv.ConvOps
import org.simbrain.util.UserParameter


/**
 * Pooling type.
 */
enum class PoolingType {
    MAX, AVERAGE
}

/**
 * Connects two [TensorLayer] nodes via a pooling operation (max or average).
 * No learnable weights - this is a purely structural downsampling operation.
 */
class PoolingConnector(
    source: TensorLayer, target: TensorLayer,
    @UserParameter(label = "Pool Size", description = "Spatial size of pooling window", displayOnly = true, order = 1)
    val poolSize: Int = 2,
    @UserParameter(label = "Stride", description = "Pooling stride", displayOnly = true, order = 2)
    val stride: Int = 2,
    @UserParameter(label = "Pooling Type", description = "MAX or AVERAGE pooling", order = 3)
    var poolingType: PoolingType = PoolingType.MAX
) : TensorConnector(source, target) {

    /** Indices of max elements for each output position (for future backprop). */
    var maxIndices: IntArray? = if (poolingType == PoolingType.MAX) IntArray(target.shape.size) else null

    init {
        val expectedShape = source.shape.poolOutputShape(poolSize, stride)
        require(target.shape == expectedShape) {
            "Target shape ${target.shape} does not match expected pool output shape $expectedShape"
        }
    }

    override fun propagate() {
        when (poolingType) {
            PoolingType.MAX -> ConvOps.maxPool2d(
                source.activations, source.shape,
                target.inputs, target.shape,
                poolSize, stride, maxIndices
            )
            PoolingType.AVERAGE -> ConvOps.avgPool2d(
                source.activations, source.shape,
                target.inputs, target.shape,
                poolSize, stride
            )
        }
    }

    /**
     * Backward pass: propagates gradient from target back to source.
     * Assumes target.gradients contains the gradient from downstream.
     *
     * Accumulates into [source].gradients. Call [source].clearGradients() before a new pass.
     */
    fun backward() {
        when (poolingType) {
            PoolingType.MAX -> {
                val indices = maxIndices ?: error("maxIndices not available for MAX pool backward")
                ConvOps.maxPool2dBackward(
                    target.gradients, target.shape,
                    indices,
                    source.gradients
                )
            }
            PoolingType.AVERAGE -> ConvOps.avgPool2dBackward(
                target.gradients, target.shape,
                source.gradients, source.shape,
                poolSize, stride
            )
        }
    }

    override val name: String get() = "Pooling"

    /** Short summary shown on the connector node in the GUI. */
    val summaryLabel: String get() = "${poolSize}x${poolSize}"

    override fun toString(): String =
        "$displayName (${poolingType.name} Pool ${poolSize}x${poolSize})"

}
