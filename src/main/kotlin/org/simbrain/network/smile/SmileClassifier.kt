package org.simbrain.network.smile

import org.simbrain.network.core.Network
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class SmileClassifier(
    val net: Network,
    val classifier: ClassifierWrapper,
    val inputSize: Int,
    val outputSize: Int,
    var nsamples: Int = 4
) : ArrayLayer(net, inputSize), EditableObject {

    /**
     * A 2d array. Rows correspond to possible inputs to the classifier.
     * Number of rows must = inputSize
     *
     * Xor example: [[0,0],[1,0],[0,1],[1,1]]
     */
    var trainingInputs: Array<DoubleArray>

    /**
     * Associates each row of traiingInputs with a classification into one of a set of categories. These can be
     * represented in different ways depending on the classifier.
     *
     * Xor example: [-1,1,1,-1]
     *
     * Simbrain will convert these "outputs" of the classifier into an appropriate double array using a one-hot
     * encoding. E.g. for a 2-category classifiers, -1 -> 1,0 and 1 -> 0,1
     */
    var targets: IntArray

    /**
     * Output matrix
     */
    private var outputs = Matrix(outputSize, 1)

    /**
     * Construct a classifier.
     */
    init {
        label = net.idManager.getProposedId(this::class.java)
        trainingInputs = Array(nsamples) { DoubleArray(inputSize) }
        targets = IntArray(nsamples)

    }

    /**
     * Train using current training data.
     */
    fun train() {
        train(trainingInputs, targets)
    }

    /**
     * Train the classiffier.
     */
    fun train(inputs: Array<DoubleArray>, targets: IntArray) {
        try {
            classifier.fit(inputs, targets)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Update the classifier by apply it to inputs and caching the result as output.
     */
    override fun update() {
        if (classifier.model != null) {
            val pred = classifier.predict(getInputs().col(0))
            println("Prediction of ${this} = $pred")
            if (classifier.model != null) {
                outputs = classifier.getOutputVector(pred, outputSize)
            }
        }
        events.fireUpdated()
        inputs.mul(0.0) // clear inputs
    }

    override fun toString(): String {
        return "${label} (${classifier.name}): $inputSize -> $outputSize"
    }

    /**
     * Get predicted output as a matrix
     */
    override fun getOutputs(): Matrix {
        return outputs
    }

    override fun outputSize(): Int {
        return outputSize
    }

    override fun getBound(): Rectangle2D? {
        return Rectangle2D.Double(x - width / 2, y - height / 2, width, height)
    }

    override fun clear() {
        inputs.mul(0.0)
        outputs.mul(0.0)
        events.fireUpdated()
    }

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator(proposedLabel: String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 4

        @UserParameter(label = "Number of outputs", order = 20)
        var nout = 2

        @UserParameter(label = "Number of training samples", order = 30)
        var nsamples = 4

        @UserParameter(label = "Classifier Type", isObjectType = true, showDetails = false, order =
        40)
        var classifierType: ClassifierWrapper = SVMClassifier()

        override fun getName(): String {
            return "Classifier"
        }

        fun create(net: Network): SmileClassifier {
            return SmileClassifier(net, classifierType, nin, nout, nsamples)
        }

    }
}