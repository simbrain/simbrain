package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow dense layer
 */
class TFDenseLayer : TFLayer<Dense>() {

    @UserParameter(label = "Number of outputs", order = 10)
    var nout = 5

    @UserParameter(label = "Activation function", order = 20)
    var activations = Activations.Relu

    @UserParameter(label = "Kernel initializer", order = 30)
    var kernelInitializer = ""

    @UserParameter(label = "Bias initializer", order = 40)
    var biasInitializer = ""

    override var layer: Dense? = null

    override fun create() : Dense {
        return Dense(nout).also {
            layer = it
        }
    }

    override fun getName(): String {
        return "Dense layer"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}