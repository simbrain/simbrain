package org.simbrain.network.core

/**
 * Describes the shape of a 3D tensor in HWC (Height, Width, Channels) layout.
 * The flat array index for element (h, w, c) is: h * rowStride + w * channels + c
 */
data class TensorShape(val height: Int, val width: Int, val channels: Int = 1) {

    init {
        require(height > 0) { "height must be > 0, but was $height" }
        require(width > 0) { "width must be > 0, but was $width" }
        require(channels > 0) { "channels must be > 0, but was $channels" }
    }

    val size: Int get() = height * width * channels

    /** Stride to move one row down (width * channels). */
    val rowStride: Int get() = width * channels

    /** Flat-array index for (h, w, c) in HWC layout. */
    inline fun index(h: Int, w: Int, c: Int): Int = h * rowStride + w * channels + c

    /**
     * Compute the output shape of a 2D convolution.
     */
    fun convOutputShape(kernelSize: Int, stride: Int, padding: Padding, numFilters: Int): TensorShape {
        require(kernelSize > 0) { "kernelSize must be > 0, but was $kernelSize" }
        require(stride > 0) { "stride must be > 0, but was $stride" }
        require(numFilters > 0) { "numFilters must be > 0, but was $numFilters" }
        val (padH, padW) = padding.compute(height, width, kernelSize, stride)
        val outH = (height + 2 * padH - kernelSize) / stride + 1
        val outW = (width + 2 * padW - kernelSize) / stride + 1
        require(outH > 0 && outW > 0) {
            "Invalid convolution output shape (${outH}x$outW) for input=$this, kernelSize=$kernelSize, stride=$stride, padding=$padding"
        }
        return TensorShape(outH, outW, numFilters)
    }

    /**
     * Compute the output shape of a pooling operation.
     */
    fun poolOutputShape(poolSize: Int, stride: Int): TensorShape {
        require(poolSize > 0) { "poolSize must be > 0, but was $poolSize" }
        require(stride > 0) { "stride must be > 0, but was $stride" }
        val outH = (height - poolSize) / stride + 1
        val outW = (width - poolSize) / stride + 1
        require(outH > 0 && outW > 0) {
            "Invalid pooling output shape (${outH}x$outW) for input=$this, poolSize=$poolSize, stride=$stride"
        }
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

        override fun toString(): String = "Valid (unpadded)"
    },
    /** Pad so output has same spatial dimensions as input (when stride=1). */
    SAME {
        override fun compute(h: Int, w: Int, kernelSize: Int, stride: Int): Pair<Int, Int> {
            val outH = (h + stride - 1) / stride
            val outW = (w + stride - 1) / stride
            val totalPadH = ((outH - 1) * stride + kernelSize - h).coerceAtLeast(0)
            val totalPadW = ((outW - 1) * stride + kernelSize - w).coerceAtLeast(0)
            // We store one symmetric pad value per axis. ceil(total/2) preserves SAME output sizing
            // when total padding would otherwise be odd.
            val padH = (totalPadH + 1) / 2
            val padW = (totalPadW + 1) / 2
            return padH to padW
        }

        override fun toString(): String = "Same (padded)"
    };

    abstract fun compute(h: Int, w: Int, kernelSize: Int, stride: Int): Pair<Int, Int>
}
