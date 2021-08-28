package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adamax
import org.jetbrains.kotlinx.dl.api.core.optimizer.Optimizer
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for tensor flow optimizers
 */
abstract class OptimizerWrapper(): CopyableObject {

     lateinit var optimizer: Optimizer

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): OptimizerWrapper

    /**
     * To get a static method
     */
    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(AdamWrapper::class.java, AdamaxWrapper::class.java)
        }
    }

}

class AdamWrapper(): OptimizerWrapper() {

    @UserParameter(label = "Blah", order = 10)
    var blah = 2

    init {
        optimizer = Adam()
    }

    override fun copy(): OptimizerWrapper {
        return AdamWrapper()
    }

    override fun getName(): String {
        return "Adam"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return OptimizerWrapper.getTypes()
        }
    }

}


class AdamaxWrapper(): OptimizerWrapper() {

    @UserParameter(label = "Blah", order = 10)
    var blah = 2

    init {
        optimizer = Adamax()
    }

    override fun copy(): OptimizerWrapper {
        return AdamaxWrapper()
    }

    override fun getName(): String {
        return "Adamax"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return OptimizerWrapper.getTypes()
        }
    }

}

