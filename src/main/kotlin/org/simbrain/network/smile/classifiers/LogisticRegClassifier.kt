package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassifierWrapper
import smile.classification.Classifier
import smile.classification.LogisticRegression
import smile.classification.SoftClassifier
import smile.math.matrix.Matrix
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile's logistic regression.
 */
class LogisticRegClassifier() : ClassifierWrapper() {

    override var model: Classifier<DoubleArray>? = null

    /**
     * Number of "output" nodes.
     */
    var targetSize: Int = 0

    /**
     * Output probabilities
     */
    lateinit var outputProbabilities: DoubleArray

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = LogisticRegression.fit(inputs, targets)
        targetSize = targets.toSet().count()
        outputProbabilities = DoubleArray(targetSize)
        val pred = model?.predict(inputs)
        stats = ""  + Accuracy.of(targets, pred)
    }

    override fun predict(input: DoubleArray): Int {
        val ret = (model as SoftClassifier).predict(input, outputProbabilities)
        // println(outputProbabilities.contentToString())
        return ret
    }

    override fun getOutputVector(result: Int, outputSize: Int): Matrix {
        return Matrix(outputProbabilities)
    }

    override fun copy(): ClassifierWrapper {
        return LogisticRegClassifier().also{
        }
    }

    override fun getName(): String {
        return "Logistic Regression"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierWrapper.getTypes()
        }
    }

}