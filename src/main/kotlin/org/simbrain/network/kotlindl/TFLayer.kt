package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.Layer
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for tensor flow layer
 */
abstract class TFLayer<T : Layer> : CopyableObject {

    override fun copy(): CopyableObject {
        TODO("Not yet implemented")
    }

    /**
     * The Kotlin DL (TensorFlow) Backing for this layer
     */
    abstract var layer: T?

    /**
     * Create a layer from parameters.
     */
    abstract fun create(): T

    /**
     * To get a static method
     */
    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(TFDenseLayer::class.java, TFFlattenLayer::class.java)
        }
    }

}