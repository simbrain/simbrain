package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.util.UserParameter
import org.simbrain.util.toLongArray

/**
 * Wrapper for tensor flow input layer
 */
class TFInputLayer(val size: Int = 5) : TFLayer<Input>() {

    // TODO: Should  argument to constructor be a shape?

    // TODO: Improve and work through cases
    @UserParameter(label = "Input shape", order = 20)
    var inputShape = intArrayOf(size,25,1)

    override var layer: Input? = null

    override fun create() : Input {
        return Input(*inputShape.toLongArray()).also {
            layer = it
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