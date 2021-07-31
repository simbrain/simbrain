package org.simbrain.network.smile.classifiers

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.simbrain.network.smile.ClassifierType
import org.simbrain.util.UserParameter

/**
 * Wrapper for Smile SVM Classifier.
 */
class RandomForestClassifier : ClassifierType() {

    @UserParameter(label = "Number of outputs", order = 10)
    var nout = 5

    @UserParameter(label = "Activation function", order = 20)
    var activations = Activations.Relu

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        return "Random Forest"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierType.getTypes()
        }
    }

}