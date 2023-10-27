package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.util.propertyeditor.GuiEditable

/**
 * Wrapper for tensor flow input layer
 */
class TFInputLayer(rows: Int = 10, cols: Int = 1, channels: Int = 1) : TFLayer<Input>() {

    var rows by GuiEditable(
        initValue = rows,
        order = 20,
        onUpdate = {
            enableWidget(layer == null)
        },
    )

    var cols by GuiEditable(
        initValue = cols,
        order = 30,
        onUpdate = {
            enableWidget(layer == null)
        }
    )

    var channels by GuiEditable(
        initValue = channels,
        order = 40,
        onUpdate = {
            enableWidget(layer == null)
        }
    )

    override var layer: Input? = null

    override fun create() : Input {
        if (cols == 1 && channels == 1) {
            // Rank 1 case
            return Input(rows.toLong()).also {
                layer = it
            }
        } else {
            // Rank 2 and 3 case
            return Input(rows.toLong(), cols.toLong(), channels.toLong()).also {
                layer = it
            }
        }
    }

    override val name = "Input layer"

    val numElements
        get() = rows * cols * channels

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(TFInputLayer::class.java)
        }
    }

}