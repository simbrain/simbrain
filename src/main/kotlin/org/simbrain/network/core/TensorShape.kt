package org.simbrain.network.core

/**
 * Describes the shape of a 3D tensor in HWC (Height, Width, Channels) layout.
 * The flat array index for element (h, w, c) is: h * wStride + w * channels + c
 */
data class TensorShape(val height: Int, val width: Int, val channels: Int = 1) {

    val size: Int get() = height * width * channels

    /** Stride to move one row down (width * channels). */
    val wStride: Int get() = width * channels

    /** Flat-array index for (h, w, c) in HWC layout. */
    inline fun index(h: Int, w: Int, c: Int): Int = h * wStride + w * channels + c

    /**
     * Compute the output shape of a 2D convolution.
     */
    fun convOutputShape(kernelSize: Int, stride: Int, padding: Padding, numFilters: Int): TensorShape {
        val (padH, padW) = padding.compute(height, width, kernelSize, stride)
        val outH = (height + 2 * padH - kernelSize) / stride + 1
        val outW = (width + 2 * padW - kernelSize) / stride + 1
        return TensorShape(outH, outW, numFilters)
    }

    /**
     * Compute the output shape of a pooling operation.
     */
    fun poolOutputShape(poolSize: Int, stride: Int): TensorShape {
        val outH = (height - poolSize) / stride + 1
        val outW = (width - poolSize) / stride + 1
        return TensorShape(outH, outW, channels)
    }

    override fun toString(): String = "${height}x${width}x${channels}"
}

/**
 * Padding strategy for convolution.
 */
enum class Padding {
    /** No padding - output shrinks. */
    VALID {
        override fun compute(h: Int, w: Int, kernelSize: Int, stride: Int) = 0 to 0
    },
    /** Pad so output has same spatial dimensions as input (when stride=1). */
    SAME {
        override fun compute(h: Int, w: Int, kernelSize: Int, stride: Int): Pair<Int, Int> {
            val padH = ((h - 1) * stride + kernelSize - h) / 2
            val padW = ((w - 1) * stride + kernelSize - w) / 2
            return padH to padW
        }
    };

    abstract fun compute(h: Int, w: Int, kernelSize: Int, stride: Int): Pair<Int, Int>
}
