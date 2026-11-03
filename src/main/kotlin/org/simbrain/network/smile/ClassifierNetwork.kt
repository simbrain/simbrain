package org.simbrain.network.smile

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.activations
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.makeBlobs
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
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
    classifier: SimbrainClassifier
) : Subnetwork(), EditableObject {

    /**
     * The wrapped classifier. It can be replaced by another with the same number of inputs and classes, for example
     * to compare models on the same data without rebuilding the network.
     */
    var classifier: SimbrainClassifier = classifier
        set(value) {
            val nInputs = inputNeuronGroup.size
            val nOutputs = outputNeuronGroup.size
            require(value.trainingData.inputs.first().size == nInputs && value.numClasses == nOutputs) {
                "Replacement classifier must have $nInputs inputs and $nOutputs classes"
            }
            field = value
            label = value.name
            winner = Integer.MIN_VALUE
            outputNeuronGroup.neuronList.activations = List(nOutputs) { 0.0 }
            events.updated.fire()
        }

    /**
     * Integer winner produced by the Smile classifier when predict is called.
     */
    var winner = Integer.MIN_VALUE

    val inputNeuronGroup = addNeuronCollection(List(classifier.trainingData.inputs.first().size) { Neuron() }).apply {
        label = "Input Layer"
        setLayoutBasedOnSize()
    }

    val outputNeuronGroup = addNeuronCollection(List(classifier.numClasses) { Neuron() }).apply {
        label = "Output Layer"
        setLayoutBasedOnSize()
        // Outputs are one-hot or probabilities, so 1 should show as fully saturated
        neuronList.forEach {
            it.upperBound = 1.0
            it.lowerBound = 0.0
        }
    }

    init {
        label = classifier.name
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
        inputNeuronGroup.isAllClamped = true
        offsetNeuronCollections(inputNeuronGroup, outputNeuronGroup, Direction.NORTH, 150.0)
        outputNeuronGroup.neuronList.forEachIndexed { i, n ->
            n.label = "Class ${i + 1}"
        }
    }
    
    /**
     * Train the classifier using the current training data.
     */
    fun train() {
        classifier.fitTrainingData()
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
        // Explicitly update input neurons (NeuronCollection has no built-in update)
        inputNeuronGroup.neuronList.forEach { it.accumulateFanInInputs() }
        inputNeuronGroup.neuronList.forEach { it.update() }
        inputNeuronGroup.neuronList.forEach { it.clearInput() }
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
                    clusterStd = 2.0
                )
                constructor.call(dataset, 0.8)
            }!!
            return ClassifierNetwork(classifier)
        }

    }
}