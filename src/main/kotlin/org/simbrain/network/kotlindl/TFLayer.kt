package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.layer.Layer
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for tensor flow layer
 */
abstract class TFLayer<T : Layer> : CopyableObject {

    // TODO: Turning these off until more clear about how to use them.

    // @get:UserParameter(label = "Number of parameters", editable = false, order = 1)
    // val numParams
    //     get() = layer?.paramCount

    // @get:UserParameter(label = "Trainable", description = "Set to false to \"freeze\" training", order = 2 )
    // var trainable
    //     get() = layer?.isTrainable
    //     set(tr) { layer?.let { it.isTrainable = tr!! }}

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
     * Returns true if the layer has not been initialized so that this layer is in "creation mode".
     * Called by reflection from a [UserParameter]
     */
    fun creationMode(): (Map<String, Any?>) -> Boolean {
        return { layer == null }
    }

    /**
     * A concept of rank usable in Simbrain. Takes the tensor flow rank, ignores the first "batch" component, and
     * ignores all components of size 1 or less.
     *
     * @See https://www.tensorflow.org/api_docs/python/tf/shape
     */
    fun getRank(): Int? {
        return layer?.let{ l ->
            val numDimsOne = l.outputShape.dims().count { it <= 1L }
            return l.outputShape.rank() - numDimsOne
        }
    }

    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(TFConv2DLayer::class.java, TFDenseLayer::class.java,
                TFAvgPool2DLayer::class.java, TFFlattenLayer::class.java)
        }
    }

}