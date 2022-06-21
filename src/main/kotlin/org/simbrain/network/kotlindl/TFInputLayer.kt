package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow input layer
 */
class TFInputLayer(
    @UserParameter(label = "Rows",  conditionalEnablingMethod = "creationMode", order = 20)
    var rows: Int = 10,
    @UserParameter(label = "Columns",  conditionalEnablingMethod = "creationMode", order = 30)
    var cols: Int = 1,
    @UserParameter(label = "Channels",  conditionalEnablingMethod = "creationMode", order = 40)
    var channels: Int = 1) : TFLayer<Input>() {

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