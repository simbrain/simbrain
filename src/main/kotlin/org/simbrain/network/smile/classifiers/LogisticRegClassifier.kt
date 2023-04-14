package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassificationAlgorithm
import org.simbrain.util.UserParameter
import smile.classification.Classifier
import smile.classification.LogisticRegression
import smile.math.matrix.Matrix
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile's logistic regression. Despite the name, it is a classifier.
 */
class LogisticRegClassifier @JvmOverloads constructor(inputSize: Int = 4, outputSize: Int = 4):
    ClassificationAlgorithm(inputSize, outputSize) {

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
        outputProbabilities = DoubleArray(outputSize)
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

    override fun getOutputVector(winner: Int): Matrix {
        assertValidWinnerIndex(winner)
        if (showProbabilities) {
            return Matrix.column(outputProbabilities)
        } else {
            return super.getOutputVector(winner)
        }
    }

    override fun copy(): ClassificationAlgorithm {
        return LogisticRegClassifier(inputSize, outputSize).also {
        }
    }

    override val name = "Logistic Regression"

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassificationAlgorithm.getTypes()
        }
    }

}