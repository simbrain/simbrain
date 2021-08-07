package org.simbrain.network.smile

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHotMat
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class SmileClassifier(
    val net: Network,
    val classifier: ClassifierWrapper,
    val inputSize: Int,
    val outputSize: Int,
    var nsamples: Int
) : Layer(), EditableObject {

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
    // TODO: Enforce these constraints

    /**
     * Collects inputs from other network models using arrays.
     */
    private val inputs: Matrix

    /**
     * Output matrix
     */
    private var outputs = Matrix(outputSize, 1)

    var width: Double = 0.0
    var height: Double = 0.0


    /**
     * Construct a classifier.
     */
    init {
        label = net.idManager.getProposedId(this::class.java)
        inputs = Matrix(inputSize, 1)
        trainingInputs = Array(nsamples) { DoubleArray(inputSize) }
        targets = IntArray(nsamples)
    }

    fun train(inputs: Array<DoubleArray>, targets: IntArray) {
        try {
            classifier.fit(inputs, targets)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun update() {
        if (classifier.model != null) {
            val pred = classifier.predict(getInputs().col(0))
            if (classifier.model != null) {
                // TODO: This is hand-coded for the SVM binary case.
                // As we get more cases expand this
                if (pred == -1) {
                    outputs = getOneHotMat(0, outputSize, 1.0)
                } else {
                    outputs = getOneHotMat(1, outputSize, 1.0)
                }
            }
        }
        events.fireUpdated()
        inputs.mul(0.0) // clear inputs
    }

    override fun toString(): String {
        return "Classifier $label"
    }

    // TODO: Get rid of this. Need not be an abstract method of Layer. Not needed here.
    override fun getInputs(): Matrix {
        return inputs
    }

    override fun addInputs(newInputs: Matrix) {
        inputs.add(newInputs)
    }

    override fun updateInputs() {
        val wtdInputs = Matrix(size(), 1)
        for (c in incomingConnectors) {
            wtdInputs.add(c.output)
        }
        addInputs(wtdInputs)
    }

    /**
     * Get predicted output as a matrix
     */
    override fun getOutputs(): Matrix {
        return outputs
    }

    override fun size(): Int {
        return inputSize
    }

    override fun getNetwork(): Network {
        return net
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

        @UserParameter(label = "Number of outputs", order = 20)
        var nout = 2

        @UserParameter(label = "Number of training samples", order = 30)
        var nsamples = 4

        @UserParameter(label = "Classifier Type", isObjectType = true, showDetails = false, order = 40)
        var classifierType = SVMClassifier()

        override fun getName(): String {
            return "Classifier"
        }

        fun create(net: Network): SmileClassifier {
            return SmileClassifier(net, classifierType, nin, nout, nsamples)
        }

    }
}