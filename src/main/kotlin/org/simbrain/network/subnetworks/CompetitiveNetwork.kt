package org.simbrain.network.subnetworks

import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix

/**
 * **CompetitiveNetwork** is a small network encompassing a Competitive
 * group. An input layer and input data have been added so that the SOM can be
 * easily trained using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
class CompetitiveNetwork : Subnetwork, UnsupervisedNetwork {

    lateinit var competitive: CompetitiveGroup

    override lateinit var trainingData: Matrix

    override lateinit var testingData: Matrix

    val defaultRowsInputData = 10

    override lateinit var inputLayer: NeuronGroup

    override val trainer = UnsupervisedTrainer()

    lateinit var weights: SynapseGroup

    constructor(numInputNeurons: Int, numCompetitiveNeurons: Int): super() {

        val initialData = Matrix.rand(defaultRowsInputData, numInputNeurons)
        val (training, testing) = splitDataSet(initialData, 0.8)
        this.trainingData = training
        this.testingData = testing

        competitive = CompetitiveGroup(numCompetitiveNeurons)
        competitive.label = "Competitive Group"
        this.addModel(competitive)
        competitive.setLayoutBasedOnSize()

        inputLayer = NeuronGroup(numInputNeurons)
        this.addModel(inputLayer)
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

    context(Network) override fun trainOnInputData() {
        trainingData.toArray().forEach { row ->
            inputLayer.activationArray = row
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
        inputLayer.update()
        // competitive group does not need to accumulate inputs because it computes weighted inputs directly
        competitive.update()
    }

    @XStreamConstructor
    constructor(): super()

    override fun randomize(randomizer: ProbabilityDistribution?) {
        weights.randomize(randomizer)
        competitive.normalizeIncomingWeights()
    }

    override fun copy(): CompetitiveNetwork {
        val copy = CompetitiveNetwork()

        // Copy competitive group
        copy.competitive = competitive.copy()
        copy.competitive.label = competitive.label
        copy.addModel(copy.competitive)

        // Copy input layer
        copy.inputLayer = inputLayer.copy()
        copy.inputLayer.label = inputLayer.label
        copy.inputLayer.isAllClamped = true
        copy.addModel(copy.inputLayer)

        // Copy weights
        copy.weights = SynapseGroup(copy.inputLayer, copy.competitive)
        // Copy weights from original synapses to new synapses
        copy.weights.synapses.zip(weights.synapses).forEach { (copyS, origS) ->
            copyS.copyFrom(origS)
        }
        copy.addModel(copy.weights)

        copy.trainer.copyFrom(trainer)

        // Copy input data
        copy.trainingData = trainingData.clone()
        copy.testingData = testingData.clone()

        return copy
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
