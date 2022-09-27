package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassifierWrapper
import smile.classification.Classifier
import smile.classification.LogisticRegression
import smile.classification.SoftClassifier
import smile.math.matrix.Matrix
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile's logistic regression. Despite the name, it is a classifier.
 */
class LogisticRegClassifier @JvmOverloads constructor(inputSize: Int = 4, outputSize: Int = 4):
    ClassifierWrapper(inputSize, outputSize) {

    override var model: Classifier<DoubleArray>? = null

    /**
     * Output probabilities
     */
    lateinit var outputProbabilities: DoubleArray

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = LogisticRegression.fit(inputs, targets)
        // targetSize = targets.toSet().count()
        outputProbabilities = DoubleArray(outputSize)
        val pred = model?.predict(inputs)
        stats = ""  + Accuracy.of(targets, pred)
    }

    override fun predict(input: DoubleArray): Int {
        val ret = (model as SoftClassifier).predict(input)
        // println(outputProbabilities.contentToString())
        return ret
    }

    override fun getOutputVector(result: Int): Matrix {
        return Matrix(outputProbabilities)
    }

    override fun copy(): ClassifierWrapper {
        return LogisticRegClassifier(inputSize, outputSize).also {
        }
    }

    override val name = "Logistic Regression"

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierWrapper.getTypes()
        }
    }

}