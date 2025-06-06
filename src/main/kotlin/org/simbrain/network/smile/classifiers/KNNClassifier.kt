package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassificationAlgorithm
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.UserParameter
import smile.classification.Classifier
import smile.classification.KNN
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile KNN Classifier.
 */
class KNNClassifier(
    dataset: ClassificationDataset = createClassificationDataset(
        inputs = mutableListOf<MutableList<Double>>(),
        targets = mutableListOf<Int>()
    ),
    splitRatio: Double = 0.8
): ClassificationAlgorithm(dataset, splitRatio) {

    @UserParameter(label = "K", order = 10)
    var k = numClasses

    override var model: Classifier<DoubleArray>? = null

    override val name: String = "K Nearest Neighbors"

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        if (k > trainingData.inputs.size) {
            throw IllegalStateException("k must be less than the number of rows in the training dataset")
        }
        model = KNN.fit(inputs, targets, k)
        setAccuracyLabel(Accuracy.of(targets, model?.predict(inputs)))
    }

    override fun predict(input: DoubleArray): Int {
        return model?.predict(input) ?: -1
    }

    override fun copy(): ClassificationAlgorithm {
        return KNNClassifier(dataset).also {
            it.k = k
        }
    }

}