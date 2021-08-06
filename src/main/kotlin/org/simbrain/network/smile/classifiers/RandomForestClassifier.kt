package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassifierWrapper
import org.simbrain.util.UserParameter
import smile.base.cart.SplitRule
import smile.classification.Classifier

/**
 * Wrapper for Smile random forest classifier.
 */
class RandomForestClassifier() : ClassifierWrapper() {

    @UserParameter(label = "Number of trees", order = 10)
    var ntrees = 5

    @UserParameter(label = "Max Depth", order = 20)
    var maxDepth = 20

    @UserParameter(label = "Split Rule", order = 20)
    var splitMethod = SplitRule.GINI

    @UserParameter(label = "Sample Rate", order = 30)
    var sampleRate = 1.0

    override var model: Classifier<DoubleArray>? = null

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        TODO("Not yet implemented")
    }

    override fun predict(input: DoubleArray): Int {
        return 0
        // return rf?.predict(input)
    }

    override fun getName(): String {
        return "Random Forest"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassifierWrapper.getTypes()
        }
    }

}