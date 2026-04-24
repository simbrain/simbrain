package org.simbrain.network.subnetworks

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.copy
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A Self-Organizing Map network with an input layer and a SOM output layer.
 *
 * The SOM layer uses neuron locations to compute neighborhood distances.
 */
class SOMNetwork : Subnetwork, UnsupervisedNetwork {

    lateinit var som: NeuronCollection

    override lateinit var inputLayer: NeuronCollection

    override val trainer = UnsupervisedTrainer()

    override lateinit var trainingData: MutableList<MutableList<Double>>

    override var testingData: MutableList<MutableList<Double>> = mutableListOf()

    // SOM params

    var initialLearningRate: Double by GuiEditable(
        label = "Initial learning rate",
        description = "Initial learning rate, which then decays",
        initValue = 0.06,
        order = 60
    )

    var initNeighborhoodSize by GuiEditable(
        label = "Initial Neighborhood size",
        description = "Initial radius around each neuron within which learning takes place",
        initValue = 100.0,
        order = 70
    )

    var learningDecayRate: Double by GuiEditable(
        label = "Learning decay rate",
        initValue = 0.002,
        description = "The rate at which the learning rate decays.",
        order = 90
    )

    var neighborhoodDecayAmount: Double by GuiEditable(
        label = "Neighborhood decay rate",
        initValue = .05,
        description = "The amount that the neighborhood decrements at each iteration",
        order = 100
    )

    // SOM runtime state

    var neighborhoodSize = 100.0
    var somLearningRate = 0.06
    var winDistance = 0.0
    var winner: Neuron? = null

    override lateinit var customInfo: InfoText

    constructor(numInputNeurons: Int, numSOMNeurons: Int) : super() {
        val somNeurons = List(numSOMNeurons) { Neuron() }
        somNeurons.forEach { n ->
            n.upperBound = 1.0
            n.lowerBound = -1.0
        }
        som = addNeuronCollection(somNeurons)
        som.label = "SOM Group"
        som.layout = HexagonalGridLayout(50.0, 50.0, sqrt(numSOMNeurons.toDouble()).toInt())
        som.applyLayout()

        inputLayer = addNeuronCollection(List(numInputNeurons) { Neuron() })
        inputLayer.setLayoutBasedOnSize()
        for (neuron in inputLayer.neuronList) {
            neuron.lowerBound = 0.0
        }
        inputLayer.label = "Input layer"
        inputLayer.isClamped = true

        trainingData = mutableListOf()

        neighborhoodSize = initNeighborhoodSize
        somLearningRate = initialLearningRate

        // Connect layers
        val sg = SynapseGroup(inputLayer, som, AllToAll())
        addModel(sg)

        customInfo = InfoText(stateInfoText)

        alignNetworkModels(inputLayer, som, Alignment.VERTICAL)
        offsetNeuronCollections(inputLayer, som, Direction.NORTH, 300.0)
    }

    @XStreamConstructor
    constructor() : super()

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network) override fun update() {
        // Update input neurons explicitly (NeuronCollection.update() is a no-op)
        inputLayer.neuronList.forEach { it.accumulateFanInInputs() }
        inputLayer.neuronList.forEach { it.update() }
        inputLayer.neuronList.forEach { it.clearInput() }
        // SOM update logic (absorbed from SOMGroup)
        updateSOM()
    }

    /**
     * Core SOM update logic. Finds winner by Euclidean distance, updates neighborhood weights.
     */
    context(Network)
    private fun updateSOM() {
        val neuronList = som.neuronList
        winDistance = Double.POSITIVE_INFINITY

        // Determine winner: SOM neuron closest to input vector
        winner = calculateWinner()
        for (i in neuronList.indices) {
            val n = neuronList[i]
            n.activation = if (n === winner) 1.0 else 0.0
        }

        if (winner == null) {
            return
        }

        // Update synapses of neurons within the neighborhood of the winner
        for (i in neuronList.indices) {
            val neuron = neuronList[i]
            val physicalDistance = getEuclideanDist(neuron, winner!!)
            if (physicalDistance <= neighborhoodSize) {
                for (incoming in neuron.fanIn) {
                    incoming.strength += somLearningRate * (incoming.source.activation - incoming.strength)
                }
            }
        }

        // Update learning rate and neighborhood size
        somLearningRate -= somLearningRate * learningDecayRate
        if (neighborhoodSize - neighborhoodDecayAmount > 0.0) {
            neighborhoodSize -= neighborhoodDecayAmount
        } else {
            neighborhoodSize = 0.0
        }

        customInfo.text = stateInfoText
        events.customInfoUpdated.fire()
    }

    /**
     * Find the SOM neuron whose weight vector is closest to the input vector.
     */
    private fun calculateWinner(): Neuron? {
        val neuronList = som.neuronList
        var winner: Neuron? = null
        for (i in neuronList.indices) {
            val n = neuronList[i]
            val distance = findDistance(n)
            if (distance < winDistance) {
                winDistance = distance
                winner = n
            }
        }
        return winner
    }

    /**
     * Euclidean distance between a SOM neuron's weight vector and the input vector.
     */
    private fun findDistance(n: Neuron): Double {
        var ret = 0.0
        for (incoming in n.fanIn) {
            ret += (incoming.strength - incoming.source.activation).pow(2.0)
        }
        return ret
    }

    val stateInfoText: String
        get() = """
            Learning rate (${Utils.round(somLearningRate, 2)})
            N-size (${Utils.round(neighborhoodSize, 2)})
        """.trimIndent()

    /**
     * Pushes the weight values of the most active SOM neuron onto the input neurons.
     */
    fun recall() {
        var maxActivation = Double.MIN_VALUE
        var mostActivatedNeuron: Neuron? = null
        for (neuron in som.neuronList) {
            if (neuron.activation > maxActivation) {
                maxActivation = neuron.activation
                mostActivatedNeuron = neuron
            }
        }
        if (mostActivatedNeuron != null) {
            for (incoming in mostActivatedNeuron.fanIn) {
                incoming.source.activation = incoming.strength
            }
        }
    }

    /**
     * Resets SOM to initial values.
     */
    fun reset() {
        somLearningRate = initialLearningRate
        neighborhoodSize = initNeighborhoodSize
        customInfo.text = stateInfoText
        events.customInfoUpdated.fire()
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
        for (n in som.neuronList) {
            for (s in n.fanIn) {
                s.lowerBound = 0.0
                s.strength = (randomizer ?: weightRandomizer).sampleDouble()
            }
        }
    }

    override fun copy(): SOMNetwork {
        val copy = SOMNetwork()

        // Copy SOM layer
        copy.som = som.copy()
        copy.som.label = som.label
        copy.addModel(copy.som)

        // Copy input layer
        copy.inputLayer = inputLayer.copy()
        copy.inputLayer.label = inputLayer.label
        copy.addModel(copy.inputLayer)

        // Copy params
        copy.initialLearningRate = initialLearningRate
        copy.initNeighborhoodSize = initNeighborhoodSize
        copy.learningDecayRate = learningDecayRate
        copy.neighborhoodDecayAmount = neighborhoodDecayAmount
        copy.neighborhoodSize = neighborhoodSize
        copy.somLearningRate = somLearningRate

        // Copy input data
        copy.trainingData = trainingData.copy()

        val neuronMap = mutableMapOf<Neuron, Neuron>()
        neuronMap.putAll(inputLayer.neuronList.zip(copy.inputLayer.neuronList))
        neuronMap.putAll(som.neuronList.zip(copy.som.neuronList))

        // Recreate connections
        val oldSg = modelList.get<SynapseGroup>().first()
        val synapseCopies = oldSg.synapses.map { oldSynapse ->
            Synapse(neuronMap[oldSynapse.source]!!, neuronMap[oldSynapse.target]!!, oldSynapse)
        }
        val sg = SynapseGroup(copy.inputLayer, copy.som, oldSg.connectionStrategy.copy(), synapseCopies.toMutableList())
        copy.addModel(sg)

        copy.trainer.copyFrom(trainer)

        copy.customInfo = InfoText(stateInfoText)

        return copy
    }

    /**
     * Helper class for creating new SOM nets.
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
