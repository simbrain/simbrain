package org.simbrain.network.subnetworks

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.SOMGroup
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.copy
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.randomMutableList
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * SOMNetwork is a  network encompassing an [SomGroup]. An input
 * layer and input data have been added so that the SOM can be easily trained
 * using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
class SOMNetwork : Subnetwork, UnsupervisedNetwork {

    lateinit var som: SOMGroup

    override lateinit var inputLayer: NeuronGroup

    override val trainer = UnsupervisedTrainer()

    override lateinit var trainingData: MutableList<MutableList<Double>>

    override var testingData: MutableList<MutableList<Double>> = mutableListOf()

    constructor(numInputNeurons: Int, numSOMNeurons: Int): super() {
        som = SOMGroup(numSOMNeurons)
        som.label = "SOM Group"
        this.addModel(som)
        som.applyLayout()

        inputLayer = NeuronGroup(numInputNeurons)
        inputLayer.setLayoutBasedOnSize()
        this.addModel(inputLayer)
        for (neuron in inputLayer.neuronList) {
            neuron.lowerBound = 0.0
        }
        inputLayer.label = "Input layer"
        inputLayer.isClamped = true

        val initialData = randomMutableList(10, numInputNeurons)
        val (training, testing) = splitDataSet(initialData, 0.8)
        this.trainingData = training
        this.testingData = testing

        // Connect layers
        val sg = SynapseGroup(inputLayer, som, AllToAll())
        addModel(sg)

        alignNetworkModels(inputLayer, som, Alignment.VERTICAL)
        offsetNeuronCollections(inputLayer, som, Direction.NORTH, 300.0)
    }

    @XStreamConstructor
    constructor(): super()

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network) override fun update() {
        inputLayer.update()
        // SOM does not need to accumulate inputs because it computes weighted inputs directly
        som.update()
    }

    context(Network) override fun trainOnInputData() {
        trainingData.forEach { row ->
            inputLayer.activationArray = row.toDoubleArray()
            trainOnCurrentPattern()
        }
    }

    context(Network) override fun trainOnCurrentPattern() {
        this.update()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        som.randomizeIncomingWeights(randomizer)
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Type: SOM Network
            Input Layer: ${inputLayer.size} neurons
            SOM Layer: ${som.size} neurons
        """.trimIndent()
    }

    override fun copy(): SOMNetwork {
        val copy = SOMNetwork()

        // Copy SOM group
        copy.som = som.copy()
        copy.som.label = som.label
        copy.addModel(copy.som)

        // Copy input layer
        copy.inputLayer = inputLayer.copy()
        copy.inputLayer.label = inputLayer.label
        copy.addModel(copy.inputLayer)

        // Copy input data
        copy.trainingData = trainingData.copy()
        copy.testingData = testingData.copy()

        val neuronMap = mutableMapOf<Neuron, Neuron>()

        neuronMap.putAll(inputLayer.neuronList.zip(copy.inputLayer.neuronList))
        neuronMap.putAll(som.neuronList.zip(copy.som.neuronList))

        // Recreate connections
        val oldSg = modelList.get<SynapseGroup>().first()
        val synapseCopies = oldSg.synapses.map { oldSynapse -> Synapse(neuronMap[oldSynapse.source]!!, neuronMap[oldSynapse.target]!!, oldSynapse) }
        val sg = SynapseGroup(copy.inputLayer, copy.som, oldSg.connectionStrategy.copy(), synapseCopies.toMutableList())
        copy.addModel(sg)

        copy.trainer.copyFrom(trainer)

        return copy
    }

    /**
     * Helper class for creating new SOM nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class SOMCreator : EditableObject {

        @UserParameter(label = "Number of som neurons", order = 10)
        var numSom: Int = 20

        @UserParameter(label = "Number of inputs", order = 20)
        var numIn: Int = 16

        fun create(): SOMNetwork {
            return SOMNetwork(numIn, numSom)
        }
    }
}
