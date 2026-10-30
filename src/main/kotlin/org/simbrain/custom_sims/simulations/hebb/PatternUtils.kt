package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.network.core.Layer
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.displayInDialog
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.util.toSimbrainColorImage
import org.simbrain.util.widgets.CorrPlotPanel
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Utils for making patterns in recurrent network simulations
 */

fun circlePattern(size: Int, bipolar: Boolean = false): DoubleArray {
    val marginPercent = 0.05
    val width = ceil(sqrt(size.toDouble())).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1
    val maxRadius = (width / 2) * (1 - marginPercent)
    val minRadius = maxRadius * 0.8 // Inner radius for unfilled effect

    return (0 until size).map { index ->
        val x = index % width
        val y = index / width
        val distance = sqrt((x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2))
        if (distance in minRadius..maxRadius) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
}

fun squarePattern(size: Int, bipolar: Boolean = false): DoubleArray {
    val marginPercent = 0.1 // 10% margin
    val width = ceil(sqrt(size.toDouble())).toInt()
    val margin = (width * marginPercent).toInt()
    val endX = width - margin
    val endY = width - margin

    return (0 until size).map { index ->
        val x = index % width
        val y = index / width
        if (
            (x == margin || x == endX - 1 || y == margin || y == endY - 1) &&
            x in margin until endX && y in margin until endY
        ) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
}

fun linePattern(size: Int, orientation: String, bipolar: Boolean = false): DoubleArray {
    val width = ceil(sqrt(size.toDouble())).toInt()

    return (0 until size).map { index ->
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
}

fun crossPattern(size: Int, bipolar: Boolean = false): DoubleArray {
    val width = ceil(sqrt(size.toDouble())).toInt()
    val centerX = (width / 2) - 1 // Center for even-sized grid
    val centerY = (width / 2) - 1

    return (0 until size).map { index ->
        val x = index % width
        val y = index / width
        if (x == centerX || y == centerY) 1.0 else (if (bipolar) -1.0 else 0.0)
    }.toDoubleArray()
}

fun applyCirclePattern(layer: Layer, bipolar: Boolean = false) =
    layer.setActivations(circlePattern(layer.size, bipolar))

fun applySquarePattern(layer: Layer, bipolar: Boolean = false) =
    layer.setActivations(squarePattern(layer.size, bipolar))

fun applyLinePattern(layer: Layer, orientation: String, bipolar: Boolean = false) =
    layer.setActivations(linePattern(layer.size, orientation, bipolar))

fun applyCrossPattern(layer: Layer, bipolar: Boolean = false) =
    layer.setActivations(crossPattern(layer.size, bipolar))

/**
 * The four built-in patterns offered by [createPatternControlPanel], keyed by their button labels.
 */
fun builtInPatterns(size: Int, bipolar: Boolean = false): Map<String, DoubleArray> = mapOf(
    "Circle" to circlePattern(size, bipolar),
    "Square" to squarePattern(size, bipolar),
    "Diagonal Line" to linePattern(size, "diagonal", bipolar),
    "Cross" to crossPattern(size, bipolar),
)

/**
 * Show a corr plot comparing a simulation's default patterns. The plot always covers every default pattern, not
 * just the ones that have been trained, so students can see in advance which patterns are likely to interfere.
 */
fun showPatternCorrelations(patterns: Map<String, DoubleArray>) =
    showPatternCorrelations(patterns.keys.toList(), patterns.values.toList())

/**
 * Show a corr plot comparing [patterns]. Patterns laid out on a square grid also get a thumbnail beside each
 * label, drawn in the network's activation colors.
 */
fun showPatternCorrelations(
    labels: List<String>,
    patterns: List<DoubleArray>,
    title: String = "Inter-pattern correlations",
) {
    require(labels.size == patterns.size) { "Expected one label per pattern" }
    if (patterns.isEmpty()) return
    val side = sqrt(patterns.first().size.toDouble()).roundToInt()
    val thumbnails = if (patterns.all { it.size == side * side }) {
        patterns.map { it.toSimbrainColorImage(side, side) }
    } else {
        null
    }
    CorrPlotPanel(labels, patterns.toTypedArray(), thumbnails).displayInDialog(isModal = false, title = title)
}

/**
 * @param trainedPatterns when given, adds a button for a corr plot of the trained patterns, and randomizing the
 * weights clears the record. The simulation's own train buttons are responsible for recording into it.
 */
suspend fun SimulationScope.createPatternControlPanel(
    layer: Layer,
    isContinuous: Boolean = false,
    trainedPatterns: TrainedPatterns? = null,
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
                trainedPatterns?.clear()
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
            addButton("Show correlations") {
                showPatternCorrelations(builtInPatterns(layer.size, isContinuous))
            }.apply {
                toolTipText = "Shows how strongly each pair of built-in patterns overlaps. Highly correlated patterns tend to interfere."
            }
            if (trainedPatterns != null) {
                addButton("Show trained correlations") {
                    trainedPatterns.showCorrelations()
                }.apply {
                    toolTipText = "Shows how strongly each pair of patterns trained so far overlaps."
                }
            }
            addSeparator()
        }
    }
}
