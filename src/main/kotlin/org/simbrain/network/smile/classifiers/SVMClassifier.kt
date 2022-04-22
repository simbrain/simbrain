package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassifierWrapper
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotMat
import smile.classification.Classifier
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import smile.math.matrix.Matrix
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile SVM Classifier.
 */
class SVMClassifier(): ClassifierWrapper() {

    // TODO: Provide separate object for selecting Kernel
    @UserParameter(label = "Polynomial Kernel Degree", order = 20)
    var kernelDegree = 2

    @UserParameter(label = "Soft margin penalty parameter", order = 30)
    var C = 1000.0

    @UserParameter(label = "Tolerance of convergence test", order = 40)
    var tolerance = 1E-3

    /**
     * The model.
     */
    override var model: Classifier<DoubleArray>? = null

    override fun copy(): ClassifierWrapper {
        return SVMClassifier().also {
            it.kernelDegree = kernelDegree
            it.C = C
            it.tolerance = tolerance
        }
    }

    override fun getName(): String {
        return "Support Vector Machine"
    }

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = SVM.fit(inputs, targets, PolynomialKernel(kernelDegree), C, tolerance)
        val pred = model?.predict(inputs)
        stats = ""  + Accuracy.of(targets, pred)
    }

    override fun predict(input: DoubleArray): Int {
        val res = model?.predict(input) ?: 0
        return if (res == -1) 0 else 1
    }

    override fun getOutputVector(result: Int, outputSize: Int): Matrix {
        return getOneHotMat(result, outputSize)
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierWrapper.getTypes()
        }
    }

}