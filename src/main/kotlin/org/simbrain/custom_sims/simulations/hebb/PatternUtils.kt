package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.stats.distributions.TwoValued
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utils for making pattern in recurrent network simulations
 */

fun applyCirclePattern(neuronList: List<Neuron>, bipolar: Boolean = false) {
    val marginPercent = 0.05
    val width = sqrt(neuronList.size.toDouble()).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1
    val maxRadius = (width / 2) * (1 - marginPercent)
    val minRadius = maxRadius * 0.8 // Inner radius for unfilled effect

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        val distance = sqrt((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
        neuron.activation = if (distance in minRadius..maxRadius) 1.0 else (if (bipolar) -1.0 else 0.0)
    }
}

fun applySquarePattern(neuronList: List<Neuron>, bipolar: Boolean = false) {
    val marginPercent = 0.1 // 10% margin
    val width = sqrt(neuronList.size.toDouble()).toInt()
    val margin = (width * marginPercent).toInt()
    val endX = width - margin
    val endY = width - margin

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = if (
            (x == margin || x == endX - 1 || y == margin || y == endY - 1) &&
            x in margin until endX && y in margin until endY
        ) 1.0 else  (if (bipolar) -1.0 else 0.0)
    }
}

fun applyLinePattern(neuronList: List<Neuron>, orientation: String, bipolar: Boolean = false) {
    val width = sqrt(neuronList.size.toDouble()).toInt()

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = when (orientation.lowercase()) {
            "horizontal" -> if (y == width / 2) 1.0 else (if (bipolar) -1.0 else 0.0)
            "vertical" -> if (x == width / 2) 1.0 else  (if (bipolar) -1.0 else 0.0)
            "diagonal" -> if (x == y) 1.0 else (if (bipolar) -1.0 else 0.0)
            "anti-diagonal" -> if (x + y == width - 1) 1.0 else (if (bipolar) -1.0 else 0.0)
            else -> throw IllegalArgumentException("Invalid orientation")
        }
    }
}

fun applyCrossPattern(neuronList: List<Neuron>, bipolar: Boolean = false) {
    val width = sqrt(neuronList.size.toDouble()).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1

    neuronList.forEachIndexed { index, neuron ->
        val x = index % width
        val y = index / width
        neuron.activation = if (x == centerX || y == centerY) 1.0 else (if (bipolar) -1.0 else 0.0)
    }
}

suspend fun SimulationScope.createPatternControlPanel(ng: NeuronGroup, isContinuous: Boolean = false): ControlPanelKt? {
    val bipolarRandomizer = TwoValued(lowerValue = -1.0, upperValue = 1.0)
    return withGui {
        createControlPanel("Control Panel", 0, 0) {
            addButton("Random Pattern") {
                ng.randomize(bipolarRandomizer)
            }
            // TODO: add back once Hopfield uses a weight matrix
            //addButton("Randomize weights") {
            //    wm.weightMatrix.randomizeSymmetric()
            //    wm.events.updated.fire()
            //}
            if (isContinuous) {
                addButton("-1 Canvas") {
                    ng.setActivationLevels(-1.0)
                }
            }
            addSeparator()
            addButton("Circle") {
                applyCirclePattern(ng.neuronList, isContinuous)
            }
            addButton("Square") {
                applySquarePattern(ng.neuronList, isContinuous)
            }
            addButton("Diagonal Line") {
                applyLinePattern(ng.neuronList, "diagonal", isContinuous)
            }
            addButton("Cross") {
                applyCrossPattern(ng.neuronList, isContinuous)
            }
            addSeparator()
        }
    }
}
