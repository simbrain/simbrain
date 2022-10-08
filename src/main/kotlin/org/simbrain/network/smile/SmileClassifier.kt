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
import kotlin.reflect.full.primaryConstructor

/**
 * Simbrain wrapper for classifiers at https://haifengl.github.io/classification.html
 *
 * To work in Simbrain, all classifiers must be able to predict a label from a double array input.
 *
 * Outputs are class labels, which are converted into a one-hot output. A number of outputs must be specified, which
 * thus corresponds to the number of class labels.
 *
 * Different classifiers are trained in different ways and thus have different properties and dialogs.
 *
 */
class SmileClassifier(
    val net: Network,
    val classifier: ClassificationAlgorithm
) : ArrayLayer(net, classifier.inputSize), EditableObject {

    /**
     * Integer winner produced by the Smile classifier when predict is called.
     */
    var winner = Integer.MIN_VALUE

    /**
     * Returns the label associated with the winning target integer.
     */
    val winningLabel: String?
        @Producible
        get() = classifier.trainingData.labelTargetMap.getInverse(winner)?:""

    /**
     * Output matrix.
     */
    private var outputs = Matrix(classifier.outputSize, 1)

    /**
     * Construct a classifier.
     */
    init {
        label = net.idManager.getProposedId(this::class.java)
    }

    /**
     * Train the classifier using the current training data.
     */
    fun train() {
        classifier.apply {
            fit(trainingData.featureVectors, trainingData.getIntegerTargets())
        }
        events.fireUpdated()
    }

    /**
     * Update the classifier by apply it to inputs and caching the result as output.
     */
    override fun update() {
        if (classifier.model != null) {
            winner = classifier.predict(inputs.col(0))
            // println("Prediction of ${this.id} is: $winner")
            if (classifier.model != null) {

                // Create output vector
                outputs = try {
                    // Assumes output of -1 is from a bipolar encoding, -1/1
                    val index = if (winner == -1) 0 else winner
                    getOneHotMat(index, outputSize())
                } catch(e: IllegalArgumentException) {
                    System.err.println(e.message)
                    Matrix(outputSize(), 1)
                }
            }
        }
        events.fireUpdated()
        inputs.mul(0.0) // clear inputs
    }

    override fun toString(): String {
        return "${label} (${classifier.name}): $classifier.inputSize -> ${outputSize()}"
    }

    /**
     * Get predicted output as a matrix
     */
    override fun getOutputs(): Matrix {
        return outputs
    }

    override fun outputSize(): Int {
        return classifier.outputSize
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

        @UserParameter(label = "Number of outputs (classes)", order = 10)
        var nout = 2

        @UserParameter(label = "Classifier Type", isObjectType = true, showDetails = false, order = 40)
        var classifierType: ClassificationAlgorithm = SVMClassifier()

        override val name = "Classifier"

        fun create(net: Network): SmileClassifier {
            return SmileClassifier(net, classifierType::class.primaryConstructor!!.call(nin, nout))
        }

        companion object {
            @JvmStatic
            fun getTypes(): List<Class<*>> {
                return ClassificationAlgorithm.getTypes()
            }
        }

    }
}