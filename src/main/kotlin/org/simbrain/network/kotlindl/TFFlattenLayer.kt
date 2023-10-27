package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten
import org.jetbrains.kotlinx.dl.api.core.shape.TensorShape
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Wrapper for tensor flow dense layer
 */
class TFFlattenLayer : TFLayer<Flatten>() {

    var nout by GuiEditable(
        label = "Number of outputs",
        initValue = 5,
        order = 10,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var activations by GuiEditable(
        initValue = Activations.Relu,
        order = 50,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    override var layer: Flatten? = null

    override fun create() : Flatten {
        return Flatten().also {
            layer = it
            it.outputShape = TensorShape(nout.toLong(), 1L)
        }
    }

    override val name = "Flatten layer"

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}