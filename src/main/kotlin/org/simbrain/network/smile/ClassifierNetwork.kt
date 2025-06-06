package org.simbrain.network.smile

import org.simbrain.network.core.Network
import org.simbrain.network.core.activations
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.ClassificationDatasetEncoding
import org.simbrain.network.trainers.makeBlobs
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
class ClassifierNetwork(
    val classifier: SimbrainClassifier
) : Subnetwork(), EditableObject {

    /**
     * Integer winner produced by the Smile classifier when predict is called.
     */
    var winner = Integer.MIN_VALUE

    /**
     * Returns the label associated with the winning target integer.
     */
    val winningLabel: String
        @Producible
        get() = classifier.trainingData.targetLabelMap.get(winner) ?: ""

    val inputNeuronGroup = NeuronGroup(classifier.trainingData.inputs.first().size).apply {
        label = "Input Layer"
        setLayoutBasedOnSize()
    }.also { addModel(it) }

    val outputNeuronGroup = NeuronGroup(classifier.numClasses).apply {
        label = "Output Layer"
        setLayoutBasedOnSize()
    }.also { addModel(it) }

    init {
        label = classifier.name
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
        inputNeuronGroup.isAllClamped = true
        offsetNeuronCollections(inputNeuronGroup, outputNeuronGroup, Direction.NORTH, 150.0)
        (classifier.trainingData.targetLabelMap.toSortedMap { k, _ -> k }.entries zip outputNeuronGroup.neuronList).forEach { (targetLabel, neuron) ->
            val (_, label) = targetLabel
            neuron.label = label
        }
    }
    
    /**
     * Train the classifier using the current training data.
     */
    fun train() {
        classifier.apply {
            fit(trainingData.inputArrays, trainingData.targetArray)
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
                    DoubleArray(classifier.numClasses) { 0.0 }
                }.toList()
            }
        }
        events.updated.fire()
    }

    override fun toString(): String {
        return "${label} (${classifier.name}): ${classifier.trainingData.inputs.first().size} -> $outputSize"
    }

    val inputSize get() = classifier.trainingData.inputs.first().size

    val outputSize get() = classifier.numClasses

    override fun copy(): Subnetwork {
        val copy = ClassifierNetwork(classifier.copy())
        copy.inputNeuronGroup.label = inputNeuronGroup.label
        copy.inputNeuronGroup.neuronList.zip(inputNeuronGroup.neuronList).forEach { (n,o) ->
            n.label = o.label
        }
        copy.outputNeuronGroup.label = outputNeuronGroup.label
        copy.outputNeuronGroup.neuronList.zip(outputNeuronGroup.neuronList).forEach { (n,o) ->
            n.label = o.label
        }
        return copy
    }

    /**
     * Helper class for creating classifiers.
     */
    class ClassifierCreator : EditableObject {

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
        var classifierType: SimbrainClassifier = SVMClassifier()

        override val name = "Classifier"

        fun create(net: Network): ClassifierNetwork {
            val classifier = classifierType::class.primaryConstructor?.let { constructor ->
                val dataset = makeBlobs(
                    nSamples = 100,
                    nCenters = nout,
                    nFeatures = nin,
                    clusterStd = 1.0,
                    encoding = if (classifierType is SVMClassifier) ClassificationDatasetEncoding.Bipolar else ClassificationDatasetEncoding.Integer
                )
                constructor.call(dataset, 0.8)
            }!!
            return ClassifierNetwork(classifier)
        }

    }
}