package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow dense layer
 */
class TFFlattenLayer : TFLayer() {

    @UserParameter(label = "Number of outputs", order = 10)
    var nout = 5

    @UserParameter(label = "Actiation function", order = 20)
    var activations = Activations.Relu

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