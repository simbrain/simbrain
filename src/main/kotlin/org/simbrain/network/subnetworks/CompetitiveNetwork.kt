package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.copy
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * A competitive network with an input layer and a competitive output layer.
 *
 * Implements Rummelhart-Zipser (PDP, 151-193) and Alvarez-Squire 1994, PNAS, 7041-7045.
 */
class CompetitiveNetwork : Subnetwork, UnsupervisedNetwork {

    lateinit var competitive: NeuronCollection

    override lateinit var trainingData: MutableList<MutableList<Double>>

    override var testingData: MutableList<MutableList<Double>> = mutableListOf()

    override lateinit var inputLayer: NeuronCollection

    override val trainer = UnsupervisedTrainer()

    lateinit var weights: SynapseGroup

    // Competitive params

    @UserParameter(label = "Update method", order = 30)
    var updateMethod = UpdateMethod.RUMM_ZIPSER

    @UserParameter(label = "Learning rate", order = 40, increment = .1)
    var learningRate = 0.1

    @UserParameter(label = "Winner Value", order = 50)
    var winValue = 1.0

    @UserParameter(label = "Lose Value", order = 60)
    var loseValue = 0.0

    @UserParameter(label = "Normalize inputs", order = 70)
    var normalizeInputs = true

    @UserParameter(label = "Use Leaky learning", order = 80)
    var useLeakyLearning = false

    var leakyLearningRate by GuiEditable(
        initValue = 0.025,
        conditionallyEnabledBy = CompetitiveNetwork::useLeakyLearning,
        order = 90
    )

    var synapseDecayPercent by GuiEditable(
        initValue = 0.0008,
        label = "Decay percent",
        description = "Percentage by which to decay synapses on each update for Alvarez-Squire update.",
        onUpdate = {
            enableWidget(widgetValue(::updateMethod) == UpdateMethod.ALVAREZ_SQUIRE)
        },
        order = 100
    )

    @UserParameter(label = "Use activation dynamics", description = "Use decay and noise in activation (from Alvarez-Squire hippocampus model)", order = 110)
    var useActivationDynamics = false

    var activationDecay by GuiEditable(
        initValue = 0.7,
        label = "Activation decay",
        description = "Decay factor for winner activation dynamics (typically 0.7)",
        conditionallyEnabledBy = CompetitiveNetwork::useActivationDynamics,
        order = 120
    )

    @UserParameter(label = "Add noise", description = "Add noise to winner activation", order = 130)
    var addNoise = false

    @Transient
    private var noiseGenerator: ProbabilityDistribution = UniformRealDistribution(-.05, .05)

    /**
     * Specific implementation of competitive learning.
     */
    enum class UpdateMethod {
        RUMM_ZIPSER {
            override fun toString() = "Rummelhart-Zipser"
        },
        ALVAREZ_SQUIRE {
            override fun toString() = "Alvarez-Squire"
        }
    }

    constructor(numInputNeurons: Int, numCompetitiveNeurons: Int) : super() {

        trainingData = mutableListOf()

        competitive = addNeuronCollection(List(numCompetitiveNeurons) { Neuron() })
        competitive.label = "Competitive Group"
        competitive.setLayoutBasedOnSize()

        inputLayer = addNeuronCollection(List(numInputNeurons) { Neuron() })
        inputLayer.label = "Input layer"
        inputLayer.isClamped = true
        inputLayer.setLayoutBasedOnSize()
        inputLayer.setLowerBound(0.0)

        weights = SynapseGroup(inputLayer, competitive)
        this.addModel(weights)
        weights.synapses.forEach { it.lowerBound = 0.0 }
        randomize()

        competitive.events.fanInUpdated.on {
            weights.events.updated.fire()
        }

        alignNetworkModels(inputLayer, competitive, Alignment.VERTICAL)
        offsetNeuronCollections(inputLayer, competitive, Direction.NORTH, 200.0)
    }

    @XStreamConstructor
    constructor() : super()

    context(Network) override fun trainOnInputData() {
        trainingData.forEach { row ->
            inputLayer.activationArray = row.toDoubleArray()
            trainOnCurrentPattern()
        }
    }

    context(Network) override fun trainOnCurrentPattern() {
        this.update()
    }

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        // Update input neurons explicitly (NeuronCollection.update() is a no-op)
        inputLayer.neuronList.forEach { it.accumulateFanInInputs() }
        inputLayer.neuronList.forEach { it.update() }
        inputLayer.neuronList.forEach { it.clearInput() }
        // Competitive update logic (absorbed from CompetitiveGroup)
        updateCompetitive()
    }

    /**
     * Core competitive update logic. Finds winner, updates activations and weights.
     */
    context(Network)
    private fun updateCompetitive() {
        val neuronList = competitive.neuronList

        // Accumulate fanIn inputs without bias
        neuronList.forEach { it.accumulateFanInInputs() }
        neuronList.forEach { it.update() }

        // Determine winner
        var max = Double.MIN_VALUE
        var winner = 0
        for (i in neuronList.indices) {
            val n = neuronList[i]
            if (n.activation > max) {
                max = n.activation
                winner = i
            }
        }

        // Update activations and weights
        for (i in neuronList.indices) {
            val neuron = neuronList[i]
            if (i == winner) {
                if (useActivationDynamics) {
                    applyActivationDynamics(neuron)
                } else {
                    neuron.activation = winValue
                }
                if (updateMethod === UpdateMethod.RUMM_ZIPSER) {
                    rummelhartZipser(neuron)
                } else if (updateMethod === UpdateMethod.ALVAREZ_SQUIRE) {
                    squireAlvarezWeightUpdate(neuron)
                    decayAllSynapses()
                }
            } else {
                neuron.activation = loseValue
                if (useLeakyLearning) {
                    leakyLearning(neuron)
                }
            }
        }
    }

    /**
     * Update winning neuron's weights in accordance with Alvarez and Squire
     * 1994, eq 2: delta_w = lambda * y * (x - x_avg)
     */
    private fun squireAlvarezWeightUpdate(neuron: Neuron) {
        for (synapse in neuron.fanIn) {
            synapse.strength +=
                learningRate * synapse.target.activation * (synapse.source.activation - synapse.target.averageInput)
        }
        competitive.events.fanInUpdated.fire()
    }

    /**
     * Update winning neuron's weights in accordance with PDP 1, p. 179.
     */
    private fun rummelhartZipser(neuron: Neuron) {
        val sumOfInputs = neuron.totalInput
        for (synapse in neuron.fanIn) {
            var activation = synapse.source.activation
            if (normalizeInputs) {
                if (sumOfInputs != 0.0) {
                    activation /= sumOfInputs
                }
            }
            synapse.strength += learningRate * (activation - synapse.strength)
        }
        competitive.events.fanInUpdated.fire()
    }

    /**
     * Decay attached synapses in accordance with Alvarez and Squire 1994, eq 3.
     */
    private fun decayAllSynapses() {
        for (n in competitive.neuronList) {
            for (synapse in n.fanIn) {
                synapse.decay(synapseDecayPercent)
            }
        }
        competitive.events.fanInUpdated.fire()
    }

    /**
     * Apply leaky learning to provided neuron.
     */
    private fun leakyLearning(neuron: Neuron) {
        val sumOfInputs = neuron.totalInput
        for (incoming in neuron.fanIn) {
            var activation = incoming.source.activation
            if (normalizeInputs) {
                if (sumOfInputs != 0.0) {
                    activation /= sumOfInputs
                }
            }
            incoming.strength = incoming.strength + leakyLearningRate * (activation - incoming.strength)
        }
        competitive.events.fanInUpdated.fire()
    }

    /**
     * Apply activation dynamics with decay and noise (from Alvarez-Squire hippocampus model).
     */
    private fun applyActivationDynamics(neuron: Neuron) {
        val noise = if (addNoise) noiseGenerator.sampleDouble() else 0.0
        val newActivation = activationDecay * neuron.activation + neuron.weightedInputs + noise
        neuron.activation = newActivation.coerceIn(0.0, 1.0)
    }

    /**
     * Normalize weights coming in to the competitive layer, separately for each neuron.
     */
    fun normalizeIncomingWeights() {
        for (n in competitive.neuronList) {
            val normFactor = n.summedIncomingWeights
            for (s in n.fanIn) {
                s.strength /= normFactor
            }
        }
        competitive.events.fanInUpdated.fire()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        competitive.randomizeIncomingWeights(randomizer ?: UniformRealDistribution(0.0, 1.0))
        normalizeIncomingWeights()
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Type: Competitive Network
            Input Layer: ${inputLayer.size} neurons
            Competitive Layer: ${competitive.size} neurons
        """.trimIndent()
    }

    override fun copy(): CompetitiveNetwork {
        val copy = CompetitiveNetwork()

        // Copy competitive layer
        copy.competitive = competitive.copy()
        copy.competitive.label = competitive.label
        copy.addNeuronCollection(copy.competitive)

        // Copy input layer
        copy.inputLayer = inputLayer.copy()
        copy.inputLayer.label = inputLayer.label
        copy.inputLayer.isAllClamped = true
        copy.addNeuronCollection(copy.inputLayer)

        // Copy weights
        copy.weights = SynapseGroup(copy.inputLayer, copy.competitive)
        copy.weights.synapses.zip(weights.synapses).forEach { (copyS, origS) ->
            copyS.copyFrom(origS)
        }
        copy.addModel(copy.weights)

        copy.trainer.copyFrom(trainer)

        // Copy params
        copy.updateMethod = updateMethod
        copy.learningRate = learningRate
        copy.winValue = winValue
        copy.loseValue = loseValue
        copy.normalizeInputs = normalizeInputs
        copy.useLeakyLearning = useLeakyLearning
        copy.leakyLearningRate = leakyLearningRate
        copy.synapseDecayPercent = synapseDecayPercent
        copy.useActivationDynamics = useActivationDynamics
        copy.activationDecay = activationDecay
        copy.addNoise = addNoise

        // Copy input data
        copy.trainingData = trainingData.copy()
        copy.testingData = testingData.copy()

        return copy
    }

    /**
     * Convenience method for setting update style from scripts.
     */
    fun setUpdateMethod(method: String) {
        if (method.equals("RZ", ignoreCase = true)) {
            updateMethod = UpdateMethod.RUMM_ZIPSER
        } else if (method.equals("AS", ignoreCase = true)) {
            updateMethod = UpdateMethod.ALVAREZ_SQUIRE
        }
    }

    /**
     * Helper class for creating new competitive nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class CompetitiveCreator : EditableObject {
        @UserParameter(label = "Number of inputs")
        var numIn: Int = 20

        @UserParameter(label = "Number of competitive neurons")
        var numComp: Int = 20

        fun create(): CompetitiveNetwork {
            return CompetitiveNetwork(numIn, numComp)
        }
    }
}
