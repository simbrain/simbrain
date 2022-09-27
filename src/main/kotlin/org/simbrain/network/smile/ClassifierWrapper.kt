package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier
import smile.math.matrix.Matrix

/**
 * Wrapper for Smile Classifier types
 */
abstract class ClassifierWrapper(
    val inputSize: Int,
    val outputSize: Int
): CopyableObject {

    // TODO: If use cases where this is not needed are found, move to subclasses and create an interface for
    //  classifiers that use this type of data
    var trainingData = ClassificationDataset(inputSize, 4)

    // TODO: This should also be made non-abstract if cases that don't use it are found
    abstract fun fit(get2DDoubleArray: Array<DoubleArray>, intColumn: IntArray)

    fun train() {
        fit(trainingData.featureVectors, trainingData.targets)
    }

    /**
     * Statistics to display after training
     */
    var stats = ""

    /**
     * The main model used for classification.
     */
    abstract var model: Classifier<DoubleArray>?

    /**
     * Use the model to generate a predicted output from inputs.
     */
    abstract fun predict(input: DoubleArray): Int

    /**
     * Convert this models integer prediction to an output vector.
     */
    abstract fun getOutputVector(result: Int): Matrix

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): ClassifierWrapper

    companion object {
        /**
         * Called via reflection using [UserParameter.typeListMethod].
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(
                LogisticRegClassifier::class.java, SVMClassifier::class.java, KNNClassifier::class.java)
        }
    }

}