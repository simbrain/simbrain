package org.simbrain.network.kotlindl

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for tensor flow layer
 */
open class TFLayer : CopyableObject {

    override fun copy(): CopyableObject {
        TODO("Not yet implemented")
    }

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