package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassificationAlgorithm
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.UserParameter
import smile.classification.Classifier
import smile.classification.LogisticRegression
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile's logistic regression. Despite the name, it is a classifier.
 */
class LogisticRegClassifier(
    dataset: ClassificationDataset = createClassificationDataset(
        inputs = mutableListOf<MutableList<Double>>(),
        targets = mutableListOf<Int>()
    ),
    splitRatio: Double = 0.8
):
    ClassificationAlgorithm(dataset, splitRatio) {

    override var model: Classifier<DoubleArray>? = null

    @UserParameter(label = "Show probabilities", description = "If true, show output probabilities rather than " +
            "a one-hot representation of the winner",
        order = 10)
    var showProbabilities = false

    /**
     * Output probabilities
     */
    lateinit var outputProbabilities: DoubleArray

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = LogisticRegression.fit(inputs, targets)
        outputProbabilities = DoubleArray(numClasses)
        val pred = model?.predict(inputs)
        setAccuracyLabel(Accuracy.of(targets, pred))
    }

    override fun predict(input: DoubleArray): Int {
        val ret: Int
        if (showProbabilities) {
            ret = model!!.predict(input, outputProbabilities)
        }  else {
            ret = model!!.predict(input)
        }
        return ret
    }

    override fun getOutputArray(winner: Int): DoubleArray {
        assertValidWinnerIndex(winner)
        return if (showProbabilities) {
            outputProbabilities
        } else {
            super.getOutputArray(winner)
        }
    }

    override fun copy(): ClassificationAlgorithm {
        return LogisticRegClassifier(dataset)
    }

    override val name = "Logistic Regression"

}