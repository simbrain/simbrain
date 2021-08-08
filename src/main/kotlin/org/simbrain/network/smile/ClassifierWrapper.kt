package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier
import smile.math.matrix.Matrix

/**
 * Wrapper for Smile Classifier types
 */
abstract class ClassifierWrapper(): CopyableObject {

    /**
     * Statistics to display after training
     */
    var stats = ""

    /**
     * The main model used for classification.
     */
    abstract var model: Classifier<DoubleArray>?

    /**
     * Train the model.
     */
    abstract fun fit(inputs: Array<DoubleArray>, targets: IntArray)

    /**
     * Use the model to generate a predicted output from inputs.
     */
    abstract fun predict(input: DoubleArray): Int

    /**
     * Convert this models integer prediction to an output vector.
     */
    abstract fun getOutputVector(result: Int, size: Int): Matrix

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): ClassifierWrapper

    /**
     * To get a static method
     */
    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(SVMClassifier::class.java, KNNClassifier::class.java, RandomForestClassifier::class.java)
        }
    }

}