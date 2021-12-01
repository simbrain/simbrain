package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.shape.TensorShape
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow dense layer
 */
class TFFlattenLayer : TFLayer<Flatten>() {

    @UserParameter(label = "Number of outputs",  conditionalEnablingMethod = "creationMode", order = 10)
    var nout = 5

    @UserParameter(label = "Activation function", order = 20)
    var activations = Activations.Relu

    override var layer: Flatten? = null

    override fun create() : Flatten {
        return Flatten().also {
            layer = it
            it.outputShape = TensorShape(nout.toLong(), 1L)
        }
    }

    override fun getName(): String {
        return "Flatten layer"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}