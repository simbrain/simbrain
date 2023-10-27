package org.simbrain.network.smile

import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.core.Network
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.toDoubleArray
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
        get() = classifier.trainingData.labelTargetMap.getInverse(winner) ?: ""

    /**
     * Output matrix.
     */
    override var outputs = Matrix(classifier.outputSize, 1)

    override val bound: Rectangle2D
        get() = Rectangle2D.Double(x - width / 2, y - height / 2, width, height)

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
        events.updated.fireAndForget()
    }

    /**
     * Update the classifier by apply it to inputs and caching the result as output.
     */
    override fun update() {
        if (classifier.model != null) {
            winner = classifier.predict(inputs.toDoubleArray())
            // println("Prediction of ${this.id} is: $winner")
            if (classifier.model != null) {
                outputs = try {
                    classifier.getOutputVector(winner)
                } catch (e: IllegalArgumentException) {
                    System.err.println(e.message)
                    Matrix(outputSize(), 1)
                }
            }
        }
        events.updated.fireAndForget()
        inputs.mul(0.0) // clear inputs
    }

    override fun toString(): String {
        return "${label} (${classifier.name}): $classifier.inputSize -> ${outputSize()}"
    }

    override fun outputSize(): Int {
        return classifier.outputSize
    }

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator(proposedLabel: String) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private var label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 4

        var nout by GuiEditable(
            initValue = 2,
            label = "Number of outputs (classes)",
            description = "Ignored for some classifiers (e.g. SVM) that can only produce 2 outputs",
            onUpdate = {
                onChange(ClassifierCreator::classifierType) {
                    enableWidget(widgetValue(ClassifierCreator::classifierType) !is SVMClassifier)
                }
            },
            order = 20
        )

        @UserParameter(label = "Classifier Type", showDetails = false, order = 40)
        var classifierType: ClassificationAlgorithm = SVMClassifier()

        override val name = "Classifier"

        fun create(net: Network): SmileClassifier {
            val classifier = classifierType::class.primaryConstructor?.let { constructor ->
                val paramMap = mapOf("inputSize" to nin, "outputSize" to nout)
                val map = constructor.parameters.associateWith { p -> paramMap[p.name] }
                constructor.callBy(map)
            }!!
            return SmileClassifier(net, classifier)
        }

        companion object {
            @JvmStatic
            fun getTypes(): List<Class<*>> {
                return ClassificationAlgorithm.getTypes()
            }
        }

    }
}