package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.activations
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.updaterules.AdditiveRule
import org.simbrain.util.place
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.stats.distributions.TwoValued
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Model forgetting dynamics. Loosely based on https://arxiv.org/abs/2112.00119
 */

val forgettingSim = newSim {

    // TODO: Better patterns. Maybe letters. Or faces.
    // TODO: Should this become the generic hopfield sim?

    val numNeurons = showNumericInputDialog("Number of neurons", 100) ?: return@newSim

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Neurons with additive nodes

    val hopfield = NeuronGroup(numNeurons).apply {
        setUpdateRule(AdditiveRule())
        applyLayout()
        toggleClamping() // Default to clamping for training
    }
    val wm = WeightMatrix(hopfield, hopfield).apply {
        learningRule = HebbianRule().apply {
            learningRate = .1
        }
        weightMatrix.randomizeSymmetric()
    }

    network.addNetworkModels(hopfield, wm).awaitAll()

    addSidebarInfo(
        """
            # Simulation of forgetting in a Hopfield-like attractor network
            
            See https://arxiv.org/abs/2112.00119
        """.trimIndent(),
        initiallyOpened = false
    )

    val bipolarRandomizer = TwoValued(-1.0, 1.0)
    var numTrainIterations = 2
    var learningRate = .01
    var forgettingRate = .1

    fun initLearningRate() {
        (wm.learningRule as HebbianRule).learningRate = learningRate
    }
    fun initForgettingRate() {
        (wm.learningRule as HebbianRule).forgettingRate = forgettingRate
    }
    initLearningRate()
    initForgettingRate()

    withGui {
        place(networkComponent, 200, 0, 509, 619)
        createControlPanel("Control Panel", 0, 0) {
            addButton("Random Pattern") {
                hopfield.randomize(bipolarRandomizer)
            }
            addButton("Randomize weights") {
                wm.weightMatrix.randomizeSymmetric()
                wm.events.updated.fire()
            }
            addSeparator()
            addButton("Circle") {
                applyCirclePattern(hopfield.neuronList, numNeurons)
            }
            addButton("Square") {
                applySquarePattern(hopfield.neuronList, numNeurons)
            }
            addButton("Diagonal Line") {
                applyLinePattern(hopfield.neuronList, numNeurons, "diagonal")
            }
            addButton("Cross") {
                applyCrossPattern(hopfield.neuronList, numNeurons)
            }
            addSeparator()
            addTextField("Learning rate", "" + learningRate) {
                it.toDoubleOrNull()?.let { num ->
                    learningRate = num
                }
                initLearningRate()
            }
            addTextField("Forgetting rate", "" + forgettingRate) {
                it.toDoubleOrNull()?.let { num ->
                    forgettingRate = num
                }
                initForgettingRate()
            }
            addSeparator()
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train") {
                // Forces into training mode
                hopfield.isAllClamped = true
                wm.clamped = false
                // Now train
                workspace.simpleIterate(numTrainIterations)
            }
            addButton("Forget") {
                repeat(numTrainIterations) {
                    (wm.learningRule as HebbianRule).applyForgetting(wm)
                }
            }
            addSeparator()
            addButton("Training Mode") {
                hopfield.isAllClamped = true
                wm.clamped = false
            }
            addButton("Retrieval Mode") {
                hopfield.isAllClamped = false
                wm.clamped = true
            }
            addButton("Neutral Mode") {
                hopfield.isAllClamped = false
                wm.clamped = false
            }
        }
    }

}

fun applyCirclePattern(neuronList: List<Neuron>, numNodes: Int) {
    val marginPercent = 0.01
    val width = sqrt(numNodes.toDouble()).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1
    val maxRadius = (width / 2) * (1 - marginPercent)
    val minRadius = maxRadius * 0.8 // Inner radius for unfilled effect

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        val distance = sqrt((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
        neuron.activation = if (distance <= maxRadius && distance >= minRadius) 1.0 else -1.0
    }
}

fun applySquarePattern(neuronList: List<Neuron>, numNodes: Int) {
    val marginPercent = 0.1 // 10% margin
    val width = sqrt(numNodes.toDouble()).toInt()
    val margin = (width * marginPercent).toInt()
    val startX = margin
    val endX = width - margin
    val startY = margin
    val endY = width - margin

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = if (
            (x == startX || x == endX - 1 || y == startY || y == endY - 1) &&
            x in startX until endX && y in startY until endY
        ) 1.0 else -1.0
    }
}

fun applyLinePattern(neuronList: List<Neuron>, numNodes: Int, orientation: String) {
    val width = sqrt(numNodes.toDouble()).toInt()

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = when (orientation.lowercase()) {
            "horizontal" -> if (y == width / 2) 1.0 else -1.0
            "vertical" -> if (x == width / 2) 1.0 else -1.0
            "diagonal" -> if (x == y) 1.0 else -1.0
            "anti-diagonal" -> if (x + y == width - 1) 1.0 else -1.0
            else -> throw IllegalArgumentException("Invalid orientation")
        }
    }
}

fun applyCrossPattern(neuronList: List<Neuron>, numNodes: Int) {
    val width = sqrt(numNodes.toDouble()).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = if (x == centerX || y == centerY) 1.0 else -1.0
    }
}

