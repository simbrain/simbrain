package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier
import smile.math.matrix.Matrix

/**
 * Superclass for wrappers of Smile classifier objects.
 */
abstract class ClassificationAlgorithm(
    val inputSize: Int,
    val outputSize: Int
): CopyableObject {


    /**
     * Main training data.
     */
    var trainingData = ClassificationDataset(inputSize, 4)

    /**
     * Fit a model to the training data.
     */
    abstract fun fit(get2DDoubleArray: Array<DoubleArray>, intColumn: IntArray)

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

    fun setAccuracyLabel(accuracy: Double) {
        stats = "Accuracy: ${Utils.round(accuracy, 3)}"
    }

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): ClassificationAlgorithm

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