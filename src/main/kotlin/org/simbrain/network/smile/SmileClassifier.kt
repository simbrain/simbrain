package org.simbrain.network.smile

import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotMat
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class SmileClassifier(
    val net: Network,
    val classifier: ClassifierWrapper,
    val inputSize: Int,
    nsamples: Int = 4
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
    var trainingTargets: IntArray = IntArray(nsamples)
        set(value) {
            field = value
            outputSize = value.toSet().count()
        }


    @UserParameter(label = "One hot encode outputs", order = 10)
    var useOneHot = true

    /**
     * The current winning class label; used in output.
     */
    var winner = 0

    /**
     * Size of outputs, currently inferred from the number of unique target labels.
     */
    var outputSize: Int = 2

    /**
     * A version of the winning class label that can be used in couplings.
     */
    @get:Producible()
    val labelEncodedOutput get() = winner.toDouble()
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
        trainingTargets = IntArray(nsamples)
    }

    /**
     * Train using current training data.
     */
    fun train() {
        train(trainingInputs, trainingTargets)
    }

    /**
     * Train the classifier.
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
            winner = classifier.predict(inputs.col(0))
            println("Prediction of ${this} = $winner")
            if (classifier.model != null) {
                if (useOneHot) {
                    outputs = getOneHotMat(winner, outputSize)
                } else {
                    outputs = classifier.getOutputVector(winner, outputSize)
                }
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

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator(proposedLabel: String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 4

        @UserParameter(label = "Classifier Type", isObjectType = true, showDetails = false, order =
        40)
        var classifierType: ClassifierWrapper = SVMClassifier()

        override val name = "Classifier"

        fun create(net: Network): SmileClassifier {
            return SmileClassifier(net, classifierType, nin)
        }

    }
}