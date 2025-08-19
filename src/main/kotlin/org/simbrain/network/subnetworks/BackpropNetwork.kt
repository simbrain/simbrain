package org.simbrain.network.subnetworks

import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.copy
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.point
import java.awt.geom.Point2D
import kotlin.math.ceil
import kotlin.math.min

/**
 * Backprop network.
 *
 * @author Jeff Yoshimi
 */
class BackpropNetwork : FeedForward, SupervisedNetwork {

    @delegate:Transient
    override val layers: LinkedHashSet<Layer> by lazy {
        computeOrderedUpdatePath(setOf(inputLayer), outputLayer)
    }

    constructor(nodesPerLayer: IntArray, initialPosition: Point2D? = point(0,0)): super(nodesPerLayer, initialPosition) {
        layerList.forEach { it.updateRule = LinearRule() }
        inputLayer.isClamped = true
        hiddenLayers().forEach {
            it.updateRule = SigmoidalRule().apply {
                type = SigmoidFunctionEnum.LOGISTIC
            }
        }
        // Good default for regression tasks
        outputLayer.updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.LOGISTIC
        }
        val nin = nodesPerLayer.first()
        val nout = nodesPerLayer.last()
        trainingSet = createDiagonalDataset(nin, nout, min(nin,nout))
        testingSet = TrainingDataset(
            inputs = MutableList(ceil(trainingSet.size * 0.2).toInt()) { MutableList(trainingSet.inputs.firstOrNull()?.size ?: 0) { 0.0 } },
            targets = MutableList(ceil(trainingSet.size * 0.2).toInt()) { MutableList(trainingSet.targets.firstOrNull()?.size ?: 0) { 0.0 } }
        )
    }

    @XStreamConstructor()
    private constructor() : super()

    override lateinit var trainingSet: TrainingDataset

    override lateinit var testingSet: TrainingDataset

    override var trainerConfig = SupervisedTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    override fun initWeights() {
        wmList.forEach { wm -> trainerConfig.weightInitializationStrategy.initializeWeights(wm) }
    }

    override fun initBiases() {
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    context(Network)
    override fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), listOf(inputLayer))
    }

    override fun toString(): String {
        val hiddenLayerSizes = hiddenLayers().map { it.size }
        val hiddenInfo = if (hiddenLayerSizes.isEmpty()) "None" 
                        else hiddenLayerSizes.joinToString(", ")
        return """
            Name: $displayName
            Type: Backprop Network
            Input Layer: ${inputLayer.size} neurons
            Hidden Layers: $hiddenInfo
            Output Layer: ${outputLayer.size} neurons
        """.trimIndent()
    }

    override fun copy(): BackpropNetwork {
        // Create a new instance with same structure
        val nodesPerLayer = layerList.map { it.size }.toIntArray()
        val copy = BackpropNetwork(nodesPerLayer)

        // Copy weights from original network
        for (i in wmList.indices) {
            copy.wmList[i].copyFrom(wmList[i])
        }

        // Copy training related properties
        copy.trainingSet = trainingSet.copy()
        copy.testingSet = testingSet.copy()
        copy.trainerConfig = SupervisedTrainerConfig().copy()

        return copy
    }

}
