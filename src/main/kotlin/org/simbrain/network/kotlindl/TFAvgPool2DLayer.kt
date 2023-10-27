package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.AvgPool2D
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Wrapper for kotlin dl Average pooling layer
 */
class TFAvgPool2DLayer : TFLayer<AvgPool2D>() {

    @UserParameter(label = "Pool size",  conditionalEnablingMethod = "creationMode", order = 20)
    var poolSize by GuiEditable(
        initValue = intArrayOf(3,3),
        order = 20,
        onUpdate = {
            enableWidget(layer != null)
        },
    )

    @UserParameter(label = "Strides",  conditionalEnablingMethod = "creationMode", order = 30)
    var strides = intArrayOf(1,1,1,1)

    @UserParameter(label = "Padding",  conditionalEnablingMethod = "creationMode", order = 80)
    var padding = ConvPadding.SAME

    override var layer: AvgPool2D? = null

    override fun create() : AvgPool2D {
        return AvgPool2D(
            poolSize = poolSize,
            strides = strides,
            padding = ConvPadding.SAME,
        ).also {
            layer = it
        }
    }

    override val name = "Average Pool 2d"

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}