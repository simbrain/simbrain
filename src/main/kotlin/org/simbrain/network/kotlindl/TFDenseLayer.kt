package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Wrapper for tensor flow dense layer
 */
class TFDenseLayer(val size: Int = 5) : TFLayer<Dense>() {

    var nout by GuiEditable(
        initValue = size,
        label = "Number of outputs",
        order = 10,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var activations by GuiEditable(
        label = "Activation function",
        initValue = Activations.Relu,
        order = 20,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var kernelInitializer by GuiEditable(
        initValue = "",
        order = 30,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var biasInitializer by GuiEditable(
        initValue = "",
        order = 40,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    override var layer: Dense? = null

    override fun create() : Dense {
        return Dense(
            nout,
            activation = activations
        ).also {
            layer = it
        }
    }

    override val name = "Dense layer"

}