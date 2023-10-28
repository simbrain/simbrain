package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.HeUniform
import org.jetbrains.kotlinx.dl.api.core.initializer.Initializer
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv2D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Wrapper for kotlin dl convolutional 2d layer
 */
class TFConv2DLayer : TFLayer<Conv2D>() {

    var nfilters by GuiEditable(
        label = "Number of filters",
        initValue = 5,
        order = 10,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var kernelSize by GuiEditable(
        initValue = intArrayOf(3,3),
        order = 20,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var strides by GuiEditable(
        initValue = intArrayOf(1,1,1,1),
        order = 30,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var dilations by GuiEditable(
        initValue = intArrayOf(1,1,1,1),
        order = 40,
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

    var padding by GuiEditable(
        initValue = ConvPadding.SAME,
        order = 80,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var useBias by GuiEditable(
        initValue = true,
        order = 90,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var kernelInitializer: Initializer = HeNormal()

    var biasInitializer: Initializer = HeUniform()

    override var layer: Conv2D? = null

    override fun create() : Conv2D {
        return Conv2D(
            filters = nfilters,
            kernelSize = kernelSize,
            strides = strides,
            dilations = dilations,
            activation = activations,
            kernelInitializer = kernelInitializer,
            biasInitializer = biasInitializer,
            padding = ConvPadding.SAME,
            useBias = useBias
        ).also {
            layer = it
        }
    }

    override val name = "Convolutional 2d"

}