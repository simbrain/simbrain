package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * A discrete Hopfield network.
 */
class Hopfield : Subnetwork, UnsupervisedNetwork {

    lateinit var neuronGroup: NeuronGroup

    override val inputLayer
        get() = neuronGroup

    lateinit var weightMatrix: WeightMatrix

    override val trainer = UnsupervisedTrainer()

    override lateinit var trainingData: MutableList<MutableList<Double>>

    override var testingData: MutableList<MutableList<Double>> = mutableListOf()

    @UserParameter(label = "Update function")
    var updateFunc = HopfieldUpdate.SYNC

    @UserParameter(label = "Learning rate")
    var learningRate = 0.25

    override lateinit var customInfo: InfoText

    constructor(numNeurons: Int): super() {

        this.trainingData = randomMutableList(10, numNeurons)

        // Create main neuron group
        neuronGroup = NeuronGroup(numNeurons)
        neuronGroup.label = "Neurons"
        neuronGroup.location = point(0.0, 0.0)
        addModel(neuronGroup)

        // Set neuron rule
        val binary = BinaryRule()
        binary.threshold = 0.0
        binary.setCeiling(1.0)
        binary.setFloor(0.0)
        neuronGroup.updateRule = binary
        neuronGroup.setIncrement(1.0)

        // Connect the neurons together
        weightMatrix = WeightMatrix(neuronGroup, neuronGroup)
        weightMatrix.label = "weights"
        addModel(weightMatrix)

        // Symmetric randomization
        // randomize() TODO()

        // Create info text
        customInfo = InfoText(stateInfoText)
        reapplyOffsets()
    }

    @XStreamConstructor
    constructor(): super()

    context(Network) override fun trainOnInputData() {
        trainingData.forEach { row ->
            neuronGroup.activationArray = row.toDoubleArray()
            trainOnCurrentPattern()
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        weightMatrix.weights.randomizeSymmetric(randomizer ?: NetworkPreferences.weightRandomizer)
        weightMatrix.events.updated.fire()
    }

    context(Network)
    override fun accumulateInputs() {
        neuronGroup.accumulateInputs()
    }

    context(Network)
    override fun update() {
        updateFunc.update(this)
        updateStateInfoText()
    }

    val stateInfoText: String
        get() = "Energy: " + getEnergy()

    fun getEnergy(): Double {
        // Convert activations to bipolar (-1, +1) for proper Hopfield energy calculation
        val bipolarActivations = neuronGroup.activations.applyFunction(::bipolar)
        return bipolarActivations.transpose()
            .mm(weightMatrix.weights)
            .mm(bipolarActivations)
            .mul(-.5)[0]
    }
    fun updateStateInfoText() {
        customInfo.text = stateInfoText
        events.customInfoUpdated.fire()
    }

    context(Network)
    override fun trainOnCurrentPattern() {
        weightMatrix.setMatrixValues(
            neuronGroup.activations
                .applyFunction(::bipolar)
                .mm(neuronGroup.activations.applyFunction(::bipolar).transpose())
                .mul(learningRate)
                .add(weightMatrix.weights)
        )
        weightMatrix.weights.zeroDiagonalInPlace()
        weightMatrix.events.updated.fire()
        events.updated.fire()
    }

    fun reapplyOffsets() {
        alignNetworkModels(neuronGroup, customInfo, Alignment.HORIZONTAL)
        val neuronGroupBound = neuronGroup.neuronList.bound
        offsetNetworkModel(neuronGroup,
            customInfo, Direction.NORTH, 40.0, neuronGroupBound.height, neuronGroupBound.width, 24.0, 0.0)
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Type: Hopfield Network
            Neurons: ${neuronGroup.size}
        """.trimIndent()
    }

    override fun copy(): Hopfield {
        val copy = Hopfield()

        // Copy neuron group and its properties
        copy.neuronGroup = neuronGroup.copy()
        copy.addModel(copy.neuronGroup)

        // Copy weight matrix
        copy.weightMatrix = WeightMatrix(copy.neuronGroup, copy.neuronGroup)
        copy.weightMatrix.label = weightMatrix.label
        copy.weightMatrix.setMatrixValues(weightMatrix.weights.clone())
        copy.addModel(copy.weightMatrix)

        // Copy other properties
        copy.updateFunc = updateFunc
        copy.learningRate = learningRate
        copy.trainingData = trainingData.copy()
        copy.testingData = testingData.copy()

        // Copy custom info
        copy.customInfo = InfoText(stateInfoText)
        copy.reapplyOffsets()

        return copy
    }

    /**
     * Main forms of Hopfield update rule.
     */
    enum class HopfieldUpdate {
        STOCHASTIC {
            /**
             * Update a single randomly chosen neuron
             */
            context(Network)
            override fun update(hop: Hopfield) {
                val randomIndex = (0 until hop.neuronGroup.size).random()
                hop.neuronGroup.neuronList[randomIndex].activation = hop.weightMatrix.weights
                    .row(randomIndex)
                    .dot(hop.neuronGroup.activationArray.applyFunctionInPlace(::bipolar))
                    .let { binary(it) }
            }

            override fun toString(): String {
                return "Stochastic"
            }
        },
        SEQ {
            /**
             * Sequential update of neurons (same sequence every time)
             */
            context(Network)
            override fun update(hop: Hopfield) {
                (0 until hop.neuronGroup.size).forEach {
                    hop.neuronGroup.neuronList[it].activation = hop.weightMatrix.weights
                        .row(it)
                        .dot(hop.neuronGroup.activationArray.applyFunctionInPlace(::bipolar))
                        .let { binary(it) }
                }
            }


            override fun toString(): String {
                return "Sequential"
            }
        },
        SYNC {
            context(Network)
            override fun update(hop: Hopfield) {
                hop.neuronGroup.setActivations(
                    hop.weightMatrix.weights
                        .mm(hop.neuronGroup.activations.applyFunctionInPlace(::bipolar))
                        .applyFunction(::binary)
                        .toDoubleArray()
                )
            }

            override fun toString(): String {
                return "Synchronous"
            }
        };

        context(Network)
        abstract fun update(hop: Hopfield)
    }

    /**
     * Helper class for creating new Hopfield nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class HopfieldCreator : EditableObject {

        /**
         * Default number of neurons.
         */
        val DEFAULT_NUM_UNITS: Int = 36

        @UserParameter(
            label = "Number of neurons",
            description = "How many neurons this Hofield net should have",
            order = -1
        )
        var numNeurons: Int = DEFAULT_NUM_UNITS

        fun create(): Hopfield {
            return Hopfield(numNeurons)
        }
    }

}