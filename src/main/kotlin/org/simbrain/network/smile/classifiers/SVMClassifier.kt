package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassifierWrapper
import org.simbrain.util.UserParameter
import smile.classification.Classifier
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile SVM Classifier.
 */
class SVMClassifier(): ClassifierWrapper() {

    // TODO: Provide separate object for selecting Kernel
    @UserParameter(label = "Polynomial Kernel Degree", order = 20)
    val kernelDegree = 2

    @UserParameter(label = "Soft margin penalty parameter", order = 30)
    var C = 1000.0

    @UserParameter(label = "Tolerance of convergence test", order = 40)
    var tolerance = 1E-3

    /**
     * The model.
     */
    override var model: Classifier<DoubleArray>? = null

    override fun getName(): String {
        return "Support Vector Machine"
    }

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = SVM.fit(inputs, targets, PolynomialKernel(kernelDegree), C, tolerance)
        val pred = model?.predict(inputs)
        stats = ""  + Accuracy.of(targets, pred)
    }

    override fun predict(input: DoubleArray): Int {
        return model?.predict(input) ?: -1
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierWrapper.getTypes()
        }
    }

}