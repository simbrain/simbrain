package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow input layer
 */
class TFInputLayer(val size: Int = 5) : TFLayer<Input>() {

    @UserParameter(label = "Number of Inputs", order = 10)
    var n_in = size

    override var layer: Input? = null

    override fun create() : Input {
        return Input(n_in.toLong()).also {
            layer = it
        }
    }

    override fun getName(): String {
        return "Input layer"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}