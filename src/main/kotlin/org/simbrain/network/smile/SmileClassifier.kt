package org.simbrain.network.smile

import org.simbrain.network.core.Network
import org.simbrain.network.core.activations
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Producible
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
    val classifier: ClassificationAlgorithm
) : Subnetwork(), EditableObject {

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

    val inputNeuronGroup = NeuronGroup(classifier.inputSize).apply {
        label = "Input Layer"
        setLayoutBasedOnSize()
    }.also { modelList.add(it) }

    val outputNeuronGroup = NeuronGroup(classifier.outputSize).apply {
        label = "Output Layer"
        setLayoutBasedOnSize()
    }.also { modelList.add(it) }

    init {
        label = classifier.name
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
        offsetNeuronCollections(inputNeuronGroup, outputNeuronGroup, Direction.NORTH, 150.0)
        when (classifier.trainingData.labelEncoding) {
            ClassificationDataset.LabelEncoding.Bipolar -> {
                outputNeuronGroup.neuronList[0].label = "Off"
                outputNeuronGroup.neuronList[1].label = "On"
            }
            ClassificationDataset.LabelEncoding.Integer -> {
                classifier.trainingData.labelTargetMap.entries.forEach { (label, index) ->
                    outputNeuronGroup.neuronList.getOrNull(index)?.label = label
                }
            }
        }
    }
    
    /**
     * Train the classifier using the current training data.
     */
    fun train() {
        classifier.apply {
            fit(trainingData.featureVectors, trainingData.getIntegerTargets())
        }
        events.updated.fire()
    }

    context(Network)
    override fun accumulateInputs() {
        inputNeuronGroup.accumulateInputs()
    }

    /**
     * Update the classifier by apply it to inputs and caching the result as output.
     */
    context(Network)
    override fun update() {
        inputNeuronGroup.update()
        if (classifier.model != null) {
            winner = classifier.predict(inputNeuronGroup.activationArray)
            // println("Prediction of ${this.id} is: $winner")
            if (classifier.model != null) {
                outputNeuronGroup.neuronList.activations = try {
                    classifier.getOutputArray(winner)
                } catch (e: IllegalArgumentException) {
                    System.err.println(e.message)
                    DoubleArray(classifier.outputSize) { 0.0 }
                }.toList()
            }
        }
        events.updated.fire()
    }

    override fun toString(): String {
        return "${label} (${classifier.name}): ${classifier.inputSize} -> $outputSize"
    }

    val inputSize get() = classifier.inputSize

    val outputSize get() = classifier.outputSize

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator() : EditableObject {

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 4

        var nout by GuiEditable(
            initValue = 2,
            label = "Number of outputs (classes)",
            description = "Ignored for some classifiers (e.g. SVM) that can only produce 2 outputs",
            onUpdate = {
                enableWidget(widgetValue(::classifierType) !is SVMClassifier)
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
            return SmileClassifier(classifier)
        }

    }
}