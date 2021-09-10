package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.HeUniform
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv2D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.simbrain.util.UserParameter
import org.simbrain.util.toLongArray

/**
 * Wrapper for kotlin dl convolutional 2d layer
 */
class TFConv2DLayer : TFLayer<Conv2D>() {

    @UserParameter(label = "Number of filters", order = 10)
    var nfilters = 5

    @UserParameter(label = "Kernel size", order = 20)
    var kernelSize = intArrayOf(3,3)

    @UserParameter(label = "Strides", order = 30)
    var strides = intArrayOf(1,1,1,1)

    @UserParameter(label = "Dilations", order = 40)
    var dilations = intArrayOf(1,1,1,1)

    @UserParameter(label = "Activation function", order = 50)
    var activations = Activations.Relu

    @UserParameter(label = "Padding", order = 80)
    var padding = ConvPadding.SAME

    @UserParameter(label = "Use bias", order = 90)
    var useBias = true

    override var layer: Conv2D? = null

    override fun create() : Conv2D {
        return Conv2D(nfilters.toLong(),
            kernelSize = kernelSize.toLongArray(),
            strides = strides.toLongArray(),
            dilations = dilations.toLongArray(),
            activation = activations,
            kernelInitializer = HeNormal(),
            biasInitializer = HeUniform(),
            padding = ConvPadding.SAME,
            useBias = useBias
        ).also {
            layer = it
        }
    }

    override fun getName(): String {
        return "Convolutional 2d"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}