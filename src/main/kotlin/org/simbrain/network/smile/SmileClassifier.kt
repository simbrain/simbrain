package org.simbrain.network.smile

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHot
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.kernel.PolynomialKernel
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class SmileClassifier(var net: Network, val classifier: SVMClassifier, inputSize: Int, outputSize: Int) :
    Layer(), EditableObject {

    // TODO
    var trainingInputs: Array<DoubleArray>
    var targets: IntArray

    @UserParameter(label = "Kernel Degree")
    private val kernelDegree = 2

    private val kernel = PolynomialKernel(kernelDegree)
    var result = 0
    var outputSize: Int

    /**
     * Collects inputs from other network models using arrays.
     */
    private val inputs: Matrix

    /**
     * Construct a classifier.
     */
    init {
        inputs = Matrix(inputSize, 1)
        val initialNumRows = 20
        trainingInputs = Array(initialNumRows) { DoubleArray(inputSize) }
        targets = IntArray(initialNumRows)
        this.outputSize = outputSize
        label = net.idManager.getProposedId(this::class.java)
    }

    fun train(inputs: Array<DoubleArray>, targets: IntArray) {
        try {
            // classifier.fit(inputs, targets, kernel, 1000.0, 1E-3)
            classifier.fit(inputs, targets)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun update() {
        // result = classifier.predict(getInputs().col(0))
    }

    override fun toString(): String {
        return "SVM classifier $label"
    }

    override fun getInputs(): Matrix {
        return inputs
    }

    override fun addInputs(newInputs: Matrix) {
        inputs.add(newInputs)
    }

    override fun getOutputs(): Matrix {
        return getOneHot(result, outputSize, 1.0)
    }

    override fun size(): Int {
        return 0
    }

    override fun getNetwork(): Network {
        return net
    }

    override fun getBound(): Rectangle2D? {
        return null
    }

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator(proposedLabel : String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 10

        @UserParameter(label = "Classifier Type", isObjectType = true, showDetails = false, order = 20)
        var classifierType = SVMClassifier()

        override fun getName(): String {
            return "Classifier"
        }

        fun create(net : Network): SmileClassifier {
            return SmileClassifier(net, classifierType, nin, 1)
        }

    }
}