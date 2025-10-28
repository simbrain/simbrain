package org.simbrain.network.subnetworks

import kotlinx.coroutines.runBlocking
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.UserParameter
import org.simbrain.util.copy
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D
import kotlin.math.ceil

/**
 *  Implements a simple recurrent network (See Elman 1990, Finding Structure in Time).
 *
 * @author Jeff Yoshimi
 */
class SRNNetwork: FeedForward, SupervisedNetwork {

    lateinit var hiddenLayer: NeuronArray

    lateinit var contextLayer: NeuronArray

    lateinit var contextToHidden: WeightMatrix

    override lateinit var trainingSet: TrainingDataset

    override lateinit var testingSet: TrainingDataset

    override lateinit var layers: LinkedHashSet<Layer>

    constructor(
        numInputNodes: Int = 10,
        numHiddenNodes: Int = 10,
        numOutputNodes: Int = 10,
        initialPosition: Point2D = point(0, 0)
    ): super(
        intArrayOf(numInputNodes, numHiddenNodes, numOutputNodes),
        initialPosition
    ) {

        inputLayer.label = "Input"
        
        hiddenLayer = layerList[1].also {
            it.updateRule = SigmoidalRule()
            it.label = "Hidden"
        }

        contextLayer = NeuronArray(numHiddenNodes).apply {
            updateRule = LinearRule()
            label = "Context"
        }
        contextLayer.fillActivations(.5)
        addModels(contextLayer)

        inputLayer.isClamped = true
        contextLayer.isClamped = true

        outputLayer.label = "Output"
        outputLayer.updateRule = SigmoidalRule()

        alignNetworkModels(inputLayer, contextLayer, Alignment.HORIZONTAL)
        offsetNetworkModel(inputLayer, contextLayer, Direction.EAST,
            100.0, 100.0, 200.0)

        contextToHidden = WeightMatrix(contextLayer, hiddenLayer)
        contextToHidden.randomize()
        addModels(contextToHidden)

        trainingSet = createDiagonalDataset(numInputNodes, numOutputNodes, shiftAmount = 1)
        testingSet = TrainingDataset(
            inputs = MutableList(ceil(trainingSet.size * 0.2).toInt()) { MutableList(trainingSet.inputs.firstOrNull()?.size ?: 0) { 0.0 } },
            targets = MutableList(ceil(trainingSet.size * 0.2).toInt()) { MutableList(trainingSet.targets.firstOrNull()?.size ?: 0) { 0.0 } }
        )

        setLocation(initialPosition.x, initialPosition.y)

        layers = computeOrderedUpdatePath(setOf(inputLayer, contextLayer), outputLayer)
    }

    @XStreamConstructor
    protected constructor() : super()

    override var trainerConfig = SRNTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    override val name: String
        get() = "SRN"

    override fun onCommit() {}

    context(Network) override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        runBlocking {
            forwardPass()
        }
        //inputLayer.update()
        //hiddenLayer.accumulateInputs()
        //hiddenLayer.update()
        //contextLayer.activations = hiddenLayer.activations.clone()
        //outputLayer.accumulateInputs()
        //outputLayer.update()
        //// Since it's expected, updating weight matrices in case learning rules have been added. In the normal case
        //// there is no such rule and these calls are bypassed.
        //wmList.forEach { it.update() }
        //contextToHidden.update()
    }

    context(Network)
    override fun forwardPass() {
        layers.forwardPass(
            listOf(inputLayer.activations, hiddenLayer.activations),
            listOf(inputLayer, contextLayer)
        )
    }

    // Forwarded from output layer
    @Producible
    fun getOutputs(): DoubleArray {
        return outputLayer.activationArray
    }

    // Forwards to input layer
    @Consumable
    open fun addInputs(inputs: DoubleArray) {
        inputLayer.addInputs(inputs)
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        super.randomize(randomizer)
        contextToHidden.randomize(randomizer)
    }

    override fun initWeights() {
        (wmList + contextToHidden).forEach { wm -> trainerConfig.weightInitializationStrategy.initializeWeights(wm) }
    }

    override fun initBiases() {
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Type: SRN Network
            Input Layer: ${inputLayer.size} neurons
            Hidden Layer: ${hiddenLayer.size} neurons
            Context Layer: ${contextLayer.size} neurons
            Output Layer: ${outputLayer.size} neurons
        """.trimIndent()
    }

    override fun copy(): SRNNetwork {
        val copy = SRNNetwork(inputLayer.size, hiddenLayer.size, outputLayer.size)

        // Copy base FeedForward structure
        copy.layerList.zip(layerList).forEach { (copyLayer, originalLayer) ->
            copyLayer.copyFrom(originalLayer)
        }
        copy.wmList.zip(wmList).forEach { (copyWeightMatrix, originalWeightMatrix) ->
            copyWeightMatrix.copyFrom(originalWeightMatrix)
        }

        // Copy context layer
        copy.contextLayer.copyFrom(contextLayer)

        // Copy context to hidden connections
        copy.contextToHidden.copyFrom(contextToHidden)

        // Copy training related properties
        copy.trainingSet = trainingSet.copy()
        copy.testingSet = testingSet.copy()
        copy.trainerConfig = SRNTrainerConfig(lossFunctionProvider = ::possibleLossFunctions).copy()

        return copy
    }

    /**
     * Helper class for creating SRN Networks.
     */
    class SRNCreator(val initialPosition: Point2D) : EditableObject {

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of hidden", order = 20)
        var nhidden = 5

        @UserParameter(label = "Number of outputs",  order = 30)
        var nout = 5

        //TODO: Node type

        override val name = "SRN Network"

        fun create(): SRNNetwork {
            return SRNNetwork(nin, nhidden, nout, initialPosition)
        }

    }
}
