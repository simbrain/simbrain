package org.simbrain.network.subnetworks

import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import java.awt.geom.Point2D

/**
 * A standard feed-forward network, as a succession of [NeuronArray] and [WeightMatrix] objects.
 *
 * @author Jeff Yoshimi
 */
open class FeedForward : Subnetwork {

    var betweenLayerInterval = 250

    /**
     * Ordered reference to [NeuronArray]'s maintained in [Subnetwork.modelList]
     */
    val layerList: MutableList<NeuronArray> = ArrayList()

    val wmList: MutableList<WeightMatrix> = ArrayList()

    lateinit var inputLayer: NeuronArray
        private set

    lateinit var outputLayer: NeuronArray
        private set

    constructor(): super()

    /**
     * @param parentNetwork Parent network
     * @param nodesPerLayer Integers 1...n correspond to number of nodes in layers 1..n
     * @param initialPosition Center location for network.
     */
    constructor(nodesPerLayer: IntArray, initialPosition: Point2D?): super() {
        inputLayer = NeuronArray(nodesPerLayer[0])
        addModel(inputLayer)
        layerList.add(inputLayer)

        // Memory of last layer created
        var lastLayer = inputLayer

        // Make hidden layers and output layer
        for (i in 1 until nodesPerLayer.size) {
            val hiddenLayer = NeuronArray(nodesPerLayer[i])
            addModel(hiddenLayer)
            layerList.add(hiddenLayer)
            offsetNetworkModel(
                lastLayer,
                hiddenLayer,
                Direction.NORTH,
                (betweenLayerInterval / 2).toDouble(),
                100.0,
                200.0
            )

            // Add weight matrix
            val wm = WeightMatrix(lastLayer, hiddenLayer)
            wm.randomize()
            addModel(wm)
            wmList.add(wm)

            // Reset last layer
            lastLayer = hiddenLayer
        }
        if (initialPosition != null) {
            setLocation(initialPosition.x, initialPosition.y)
        }
        outputLayer = lastLayer
    }

    override val name: String
        get() = "Feedforward"

    override fun onCommit() {}

    override fun randomize(randomizer: ProbabilityDistribution?) {
        wmList.forEach { wm -> wm.randomize(NormalDistribution(0.0, .1)) }
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases(NormalDistribution(0.0, .01))
        }
    }

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        inputLayer.update()
        for (i in 1 until layerList.size - 1) {
            layerList[i].accumulateInputs()
            layerList[i].update()
        }
        outputLayer.accumulateInputs()
        outputLayer.update()
    }

    fun hiddenLayers() = layerList.drop(1).take(layerList.size-2)

    override fun copy(): FeedForward {
        val copy = FeedForward()
        copy.betweenLayerInterval = betweenLayerInterval

        // Copy input layer
        copy.inputLayer = inputLayer.copy()
        copy.addModel(copy.inputLayer)
        copy.layerList.add(copy.inputLayer)

        // Memory of last layer created
        var lastLayer = copy.inputLayer

        // Copy hidden layers and output layer
        for (i in 1 until layerList.size) {
            val layer = layerList[i].copy()
            copy.addModel(layer)
            copy.layerList.add(layer)

            // Maintain layout
            offsetNetworkModel(
                lastLayer,
                layer,
                Direction.NORTH,
                (betweenLayerInterval / 2).toDouble(),
                100.0,
                200.0
            )

            // Copy weight matrix
            val wm = WeightMatrix(lastLayer, layer)
            wm.setMatrixValues(wmList[i-1].weights.clone())
            copy.addModel(wm)
            copy.wmList.add(wm)

            lastLayer = layer
        }

        copy.outputLayer = lastLayer
        return copy
    }
}
