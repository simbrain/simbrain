package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.ActivationLayer
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow activation layer
 */
class TFActivationLayer(val size: Int = 5) : TFLayer<ActivationLayer>() {

    @UserParameter(label = "Activation function", order = 20)
    var activations = Activations.Relu

    override var layer: ActivationLayer? = null

    override fun create() : ActivationLayer {
        return ActivationLayer(
            activation = activations
        ).also {
            layer = it
        }
    }

    override val name = "Activation layer"

}