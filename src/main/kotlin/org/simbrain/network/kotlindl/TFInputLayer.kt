package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.util.UserParameter
import org.simbrain.util.toLongArray

/**
 * Wrapper for tensor flow input layer
 */
class TFInputLayer(val rows: Int = 10, val cols: Int = 1, val channels: Int = 1) : TFLayer<Input>() {

    // TODO: Possibly break out into three scalar parameters
    @UserParameter(label = "Input shape",  conditionalEnablingMethod = "creationMode", order = 20)
    var inputShape = intArrayOf(rows, cols, channels)

    override var layer: Input? = null

    override fun create() : Input {
        // Hack to convert from shape array to var-args.
        // TODO.
        if (cols <= 1) {
            return Input(inputShape[0].toLong()).also {
                layer = it
            }
        } else if (channels <= 1) {
            return Input(inputShape[0].toLong(), inputShape[1].toLong()).also {
                layer = it
            }
        } else {
            return Input(inputShape[0].toLong(), inputShape[1].toLong(), inputShape[2].toLong()).also {
                layer = it
            }
        }
    }

    override fun getName(): String {
        return "Input layer"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(TFInputLayer::class.java)
        }
    }

}