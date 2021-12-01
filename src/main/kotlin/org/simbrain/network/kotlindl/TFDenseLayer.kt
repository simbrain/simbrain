package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow dense layer
 */
class TFDenseLayer(val size: Int = 5) : TFLayer<Dense>() {

    @UserParameter(label = "Number of outputs",  conditionalEnablingMethod = "creationMode", order = 10)
    var nout = size

    @UserParameter(label = "Activation function",  conditionalEnablingMethod = "creationMode",  order = 20)
    var activations = Activations.Relu

    @UserParameter(label = "Kernel initializer",  conditionalEnablingMethod = "creationMode", order = 30)
    var kernelInitializer = ""

    @UserParameter(label = "Bias initializer",  conditionalEnablingMethod = "creationMode", order = 40)
    var biasInitializer = ""

    override var layer: Dense? = null

    override fun create() : Dense {
        return Dense(
            nout,
            activation = activations
        ).also {
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