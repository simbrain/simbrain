/**
 * Wrapper for Smile's support vector machine. Smile's SVM is binary and expects -1/1 labels, so the wrapper maps the
 * dataset's labels onto -1/1 for two classes and trains one SVM per class (one versus rest) for more than two.
 * Predictions are returned in the dataset's own labels.
 */
package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.SimbrainClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.ClassificationDatasetEncoding
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotArray
import smile.classification.Classifier
import smile.classification.OneVersusRest
import smile.classification.SVM
import smile.math.kernel.GaussianKernel
import smile.math.kernel.LinearKernel
import smile.math.kernel.MercerKernel
import smile.math.kernel.PolynomialKernel
import smile.validation.metric.Accuracy

class SVMClassifier(
    dataset: ClassificationDataset = createClassificationDataset(
        inputs = mutableListOf<MutableList<Double>>(),
        targets = mutableListOf(-1, 1),
        encoding = ClassificationDatasetEncoding.Bipolar
    ),
    splitRatio: Double = 0.8
):
    SimbrainClassifier(dataset, splitRatio) {

    enum class KernelType(val description: String) {
        Gaussian("Gaussian (RBF)"),
        Polynomial("Polynomial"),
        Linear("Linear");

        override fun toString() = description
    }

    @UserParameter(
        label = "Kernel",
        description = "Linear gives a straight boundary. Polynomial and Gaussian give curved boundaries.",
        order = 10
    )
    var kernelType = KernelType.Gaussian

    @UserParameter(
        label = "Gaussian kernel width",
        description = "Used by the Gaussian kernel. Small widths give tight, wiggly boundaries around individual " +
                "points; large widths give smooth boundaries.",
        minimumValue = 0.0,
        order = 15
    )
    var kernelWidth = 1.0

    @UserParameter(label = "Polynomial kernel degree", description = "Used by the polynomial kernel.", order = 20)
    var kernelDegree = 2

    @UserParameter(
        label = "Soft margin penalty parameter",
        description = "How heavily misclassified training points are penalized. Large values fit the training data " +
                "closely; small values allow some errors in exchange for a wider, smoother margin.",
        order = 30
    )
    var C = 1.0

    @UserParameter(label = "Tolerance of convergence test", order = 40)
    var tolerance = 1E-3

    override var model: Classifier<DoubleArray>? = null

    /**
     * Sorted labels seen during training. Index i of the one-hot output corresponds to `labels[i]`.
     */
    private var labels = intArrayOf()

    override fun copy(): SimbrainClassifier {
        return SVMClassifier(dataset.copy()).also {
            it.kernelType = kernelType
            it.kernelWidth = kernelWidth
            it.kernelDegree = kernelDegree
            it.C = C
            it.tolerance = tolerance
        }
    }

    override val name = "Support Vector Machine"

    private fun createKernel(): MercerKernel<DoubleArray> = when (kernelType) {
        KernelType.Gaussian -> GaussianKernel(kernelWidth)
        KernelType.Polynomial -> PolynomialKernel(kernelDegree)
        KernelType.Linear -> LinearKernel()
    }

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        labels = targets.distinct().sorted().toIntArray()
        require(labels.size >= 2) { "SVM training data must contain at least two classes" }
        val kernel = createKernel()
        model = if (labels.size == 2) {
            val bipolarTargets = IntArray(targets.size) { if (targets[it] == labels[1]) 1 else -1 }
            BinaryModel(SVM.fit(inputs, bipolarTargets, kernel, C, tolerance), labels)
        } else {
            OneVersusRest.fit(inputs, targets) { x, y -> SVM.fit(x, y, kernel, C, tolerance) }
        }
        setTrainingStats(Accuracy.of(targets, model?.predict(inputs)))
        setTestingStats(Accuracy.of(testingData.targetArray, model?.predict(testingData.inputArrays)))
    }

    override fun predict(input: DoubleArray): Int {
        // Todo: null to -1 seems problematic
        return model?.predict(input) ?: -1
    }

    override fun classIndex(label: Int) = labels.indexOf(label).coerceAtLeast(0)

    override fun getOutputArray(winner: Int): DoubleArray {
        return getOneHotArray(classIndex(winner), numClasses)
    }

    /**
     * Binary SVM whose -1/1 predictions are reported as the dataset's own two labels.
     */
    private class BinaryModel(val svm: Classifier<DoubleArray>, val labels: IntArray) : Classifier<DoubleArray> {
        override fun numClasses() = 2
        override fun classes() = labels
        override fun predict(x: DoubleArray) = if (svm.predict(x) > 0) labels[1] else labels[0]
    }

}
