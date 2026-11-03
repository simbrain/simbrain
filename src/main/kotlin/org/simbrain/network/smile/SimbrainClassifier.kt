package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.split
import org.simbrain.util.Utils
import org.simbrain.util.getOneHotArray
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier

/**
 * Superclass for wrappers of Smile classifier objects.
 */
abstract class SimbrainClassifier(
    val dataset: ClassificationDataset,
    splitRatio: Double = 0.8,
): CopyableObject {

    var trainingData: ClassificationDataset
    var testingData: ClassificationDataset

    init {
        val (training, testing) = dataset.split(splitRatio)
        this.trainingData = training
        this.testingData = testing
    }

    val numClasses: Int get() = trainingData.numClasses

    /**
     * Fit a model to the training data.
     */
    abstract fun fit(get2DDoubleArray: Array<DoubleArray>, intColumn: IntArray)

    /**
     * Statistics to display after training
     */
    var trainingStats = ""

    /**
     * Fraction of the training data classified correctly by the last fit, or NaN before training.
     */
    var trainingAccuracy = Double.NaN
        private set

    /**
     * Fraction of the testing data classified correctly by the last fit, or NaN before training.
     */
    var testingAccuracy = Double.NaN
        private set

    var testingStats = ""

    /**
     * The main model used for classification.
     */
    abstract var model: Classifier<DoubleArray>?

    /**
     * Use the model to generate a predicted output from inputs.
     */
    abstract fun predict(input: DoubleArray): Int

    fun assertValidWinnerIndex(winner: Int) {
        if (winner > numClasses) {
            throw IllegalArgumentException("Prediction of ${winner} > output size of ${numClasses}")
        }
    }

    /**
     * Index of a predicted label among the classes, used to pick the active output neuron or a plot color.
     */
    open fun classIndex(label: Int) = label

    /**
     * Convert this model's integer prediction to an output vector.
     */
    open fun getOutputArray(winner: Int): DoubleArray {
        assertValidWinnerIndex(winner)
        return getOneHotArray(winner, numClasses)
    }

    fun setTrainingStats(accuracy: Double) {
        trainingAccuracy = accuracy
        trainingStats = "Accuracy: ${Utils.round(accuracy, 3)}"
    }

    fun setTestingStats(accuracy: Double) {
        testingAccuracy = accuracy
        testingStats = "Accuracy: ${Utils.round(accuracy, 3)}"
    }

    /**
     * Fit the model to [trainingData].
     */
    fun fitTrainingData() = fit(trainingData.inputArrays, trainingData.targetArray)

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): SimbrainClassifier

    override fun getTypeList() = classifierTypes

}

val classifierTypes = listOf(
    LogisticRegClassifier::class.java,
    SVMClassifier::class.java,
    KNNClassifier::class.java,
    RandomForestClassifier::class.java
)