package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.network.core.Layer
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.stats.distributions.TwoValued
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utils for making patterns in recurrent network simulations
 */

fun applyCirclePattern(layer: Layer, bipolar: Boolean = false) {
    val marginPercent = 0.05
    val width = ceil(sqrt(layer.size.toDouble())).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1
    val maxRadius = (width / 2) * (1 - marginPercent)
    val minRadius = maxRadius * 0.8 // Inner radius for unfilled effect

    val pattern = (0 until layer.size).map { index ->
        val x = index % width
        val y = index / width
        val distance = sqrt((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
        if (distance in minRadius..maxRadius) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
    layer.setActivations(pattern)
}

fun applySquarePattern(layer: Layer, bipolar: Boolean = false) {
    val marginPercent = 0.1 // 10% margin
    val width = ceil(sqrt(layer.size.toDouble())).toInt()
    val margin = (width * marginPercent).toInt()
    val endX = width - margin
    val endY = width - margin

    val pattern = (0 until layer.size).map { index ->
        val x = index % width
        val y = index / width
        if (
            (x == margin || x == endX - 1 || y == margin || y == endY - 1) &&
            x in margin until endX && y in margin until endY
        ) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
    layer.setActivations(pattern)
}

fun applyLinePattern(layer: Layer, orientation: String, bipolar: Boolean = false) {
    val width = ceil(sqrt(layer.size.toDouble())).toInt()

    val pattern = (0 until layer.size).map { index ->
        val x = index % width
        val y = index / width
        when (orientation.lowercase()) {
            "horizontal" -> if (y == width / 2) 1.0 else (if (bipolar) -1.0 else 0.0)
            "vertical" -> if (x == width / 2) 1.0 else (if (bipolar) -1.0 else 0.0)
            "diagonal" -> if (x == y) 1.0 else (if (bipolar) -1.0 else 0.0)
            "anti-diagonal" -> if (x + y == width - 1) 1.0 else (if (bipolar) -1.0 else 0.0)
            else -> throw IllegalArgumentException("Invalid orientation")
        }
    }.toDoubleArray()
    layer.setActivations(pattern)
}

fun applyCrossPattern(layer: Layer, bipolar: Boolean = false) {
    val width = ceil(sqrt(layer.size.toDouble())).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1

    val pattern = (0 until layer.size).map { index ->
        val x = index % width
        val y = index / width
        if (x == centerX || y == centerY) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
    layer.setActivations(pattern)
}

suspend fun SimulationScope.createPatternControlPanel(
    layer: Layer,
    isContinuous: Boolean = false,
    randomizeWeights: () -> Unit = {},
): ControlPanelKt? {
    return withGui {
        createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            val offValue = if (isContinuous) -1.0 else 0.0
            addButton("Clear pattern") {
                layer.setActivations(DoubleArray(layer.size) { offValue })
            }.apply {
                toolTipText = "Sets every neuron to $offValue, giving a blank canvas."
            }
            addButton("Random pattern") {
                layer.randomize(TwoValued(offValue, 1.0))
            }.apply {
                toolTipText = "Sets each neuron randomly to $offValue or 1.0."
            }
            addButton("Randomize weights") {
                randomizeWeights()
            }.apply {
                toolTipText = "Randomizes the weights, erasing any stored patterns."
            }
            addSeparator()
            addButton("Circle") {
                applyCirclePattern(layer, isContinuous)
            }.apply {
                toolTipText = "Sets the neurons to a ring pattern."
            }
            addButton("Square") {
                applySquarePattern(layer, isContinuous)
            }.apply {
                toolTipText = "Sets the neurons to a square outline pattern."
            }
            addButton("Diagonal Line") {
                applyLinePattern(layer, "diagonal", isContinuous)
            }.apply {
                toolTipText = "Sets the neurons to a diagonal line pattern."
            }
            addButton("Cross") {
                applyCrossPattern(layer, isContinuous)
            }.apply {
                toolTipText = "Sets the neurons to a cross pattern."
            }
            addSeparator()
        }
    }
}
