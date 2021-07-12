package org.simbrain.network.kotlindl

import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for tensor flow layer
 */
open class TFLayer : CopyableObject {

    // /**
    //  * Called via reflection using [UserParameter.typeListMethod].
    //  */
    // fun getTypes(): List<Class<*>> {
    //     return listOf(TFDenseLayer::class.java)
    // }

    override fun copy(): CopyableObject {
        TODO("Not yet implemented")
    }

}