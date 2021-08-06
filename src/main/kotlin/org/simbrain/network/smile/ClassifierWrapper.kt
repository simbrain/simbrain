package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier

/**
 * Wrapper for Smile Classifier types
 */
abstract class ClassifierWrapper(): CopyableObject {

    /**
     * Statistics set after training
     */
    var stats = ""

    abstract var model: Classifier<DoubleArray>?

    override fun copy(): CopyableObject {
        TODO("Not yet implemented")
    }

    /**
     * Train the model.
     */
    abstract fun fit(inputs: Array<DoubleArray>, targets: IntArray)

    /**
     * Use the model.
     */
    abstract fun predict(input: DoubleArray): Int

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