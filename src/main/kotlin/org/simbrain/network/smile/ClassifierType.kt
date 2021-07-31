package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Wrapper for Smile Classifier types
 */
abstract class ClassifierType : CopyableObject {

    override fun copy(): CopyableObject {
        TODO("Not yet implemented")
    }

    abstract fun fit(inputs: Array<DoubleArray>, targets: IntArray)

    /**
     * To get a static method
     */
    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(SVMClassifier::class.java, RandomForestClassifier::class.java)
        }
    }

}