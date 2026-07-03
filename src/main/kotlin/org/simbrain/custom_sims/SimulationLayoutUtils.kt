package org.simbrain.custom_sims

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.network.core.Network
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.ControlPanelKt
import java.awt.Component
import kotlin.math.sqrt

const val SIM_NEURON_INTERVAL = 30

const val SIM_WINDOW_GAP = 10

const val COMPETITIVE_LAYER_GAP = 140.0

const val SOM_SIM_SOM_INTERVAL = 34

const val SOM_SMELLS_SOM_INTERVAL = 38

const val SOM_LAYER_GAP_EXTRA = 100.0

fun Component.rightEdgeWithGap(gap: Int = SIM_WINDOW_GAP) = x + width + gap

fun Component.bottomEdgeWithGap(gap: Int = SIM_WINDOW_GAP) = y + height + gap

fun Component.centeredXInColumn(columnX: Int, columnWidth: Int) = columnX + ((columnWidth - width) / 2).coerceAtLeast(0)

suspend fun ControlPanelKt.awaitLayout() = apply {
    withContext(Dispatchers.Swing) {
        pack()
    }
}

fun CompetitiveNetwork.showInputPattern(network: Network, pattern: List<Double>) {
    inputLayer.setActivations(pattern.toDoubleArray())
    val savedLearningRate = learningRate
    learningRate = 0.0
    with(network) { update() }
    learningRate = savedLearningRate
}

fun CompetitiveNetwork.applySimulationLayout(
    neuronInterval: Int = SIM_NEURON_INTERVAL,
    layerGap: Double = COMPETITIVE_LAYER_GAP,
) {
    inputLayer.betweenNeuronInterval = neuronInterval
    competitive.betweenNeuronInterval = neuronInterval
    inputLayer.setLayoutBasedOnSize()
    inputLayer.applyLayout()
    competitive.setLayoutBasedOnSize()
    competitive.applyLayout()
    alignNetworkModels(inputLayer, competitive, Alignment.VERTICAL)
    offsetNeuronCollections(inputLayer, competitive, Direction.NORTH, layerGap)
}

fun SOMNetwork.applySimulationLayout(
    inputNeuronInterval: Int = SIM_NEURON_INTERVAL,
    somNeuronInterval: Int = inputNeuronInterval,
    layerGapFraction: Double = 0.5,
) {
    inputLayer.betweenNeuronInterval = inputNeuronInterval
    inputLayer.setLayoutBasedOnSize()
    inputLayer.applyLayout()
    som.layout = HexagonalGridLayout(
        somNeuronInterval.toDouble(),
        somNeuronInterval.toDouble(),
        sqrt(som.size.toDouble()).toInt(),
    )
    som.applyLayout()
    val somHeight = som.maxY - som.minY
    val layerGap = somHeight * layerGapFraction + SOM_LAYER_GAP_EXTRA
    alignNetworkModels(inputLayer, som, Alignment.VERTICAL)
    offsetNeuronCollections(inputLayer, som, Direction.NORTH, layerGap)
}
