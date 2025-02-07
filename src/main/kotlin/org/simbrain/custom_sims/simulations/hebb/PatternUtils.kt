package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.network.core.Layer
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.distributions.TwoValued
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utils for making pattern in recurrent network simulations
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
        createControlPanel("Control Panel", 0, 0) {
            addButton("Random pattern") {
                if (isContinuous) {
                    layer.randomize(TwoValued(-1.0, 1.0))
                } else {
                    layer.randomize(TwoValued(0.0, 1.0))
                }
            }
            addButton("Randomize parameters") {
                randomizeWeights()
            }
            if (isContinuous) {
                addButton("-1 Canvas") {
                    layer.setActivations(DoubleArray(layer.size) { -1.0 })
                }
            }
            addSeparator()
            addButton("Circle") {
                applyCirclePattern(layer, isContinuous)
            }
            addButton("Square") {
                applySquarePattern(layer, isContinuous)
            }
            addButton("Diagonal Line") {
                applyLinePattern(layer, "diagonal", isContinuous)
            }
            addButton("Cross") {
                applyCrossPattern(layer, isContinuous)
            }
            addSeparator()
        }
    }
}

class PatternTester: EditableObject {

    var distancePercentThreshold by GuiEditable(
        label = "Distance Threshold",
        initValue = 5.0,
        min = 0.0,
        max = 100.0,
        increment = 1.0,
        order = 10
    )

    var percentToTest by GuiEditable(
        label = "Percent to Test",
        initValue = 30.0,
        min = 0.0,
        max = 100.0,
        order = 20
    )

}

fun hammingDistance(actual: DoubleArray, expected: DoubleArray): Double {
    return actual.zip(expected).count { (a, b) -> a != b }.toDouble()
}

fun signHammingDistance(actual: DoubleArray, expected: DoubleArray): Double {
    return actual.zip(expected).sumOf { (a, b) ->
        val sameSign = a * b >= 0
        if (!sameSign) return@sumOf 1.0
        if (abs(a) > 1.0) return@sumOf 0.0
        return@sumOf abs(a - b)
    }
}

context(SimulationScope)
fun ControlPanelKt.createHopfieldTestPane(
    hopfield: Layer,
    applyTraining: suspend () -> Unit,
    applyLearningRate: (learningRate: Double) -> Unit,
    applyReset: () -> Unit,
    distanceFunction: (actual: DoubleArray, expected: DoubleArray) -> Double = ::hammingDistance,
    buttonName: String = "Capacity Test",
) {

    val patternTester = PatternTester()


    val patternTesterEditor = AnnotatedPropertyEditor(patternTester)
    addAnnotatedPropertyEditor(patternTesterEditor)

    addButton("Apply Config") {
        patternTesterEditor.commitChanges()
    }

    fun computeNTests(): Int = (patternTester.percentToTest / 100 * hopfield.size).toInt()

    fun applyRandomPattern(hopfield: Layer): DoubleArray {
        hopfield.randomize(TwoValued(-1.0, 1.0))
        return hopfield.activationArray
    }

    val allPatterns = (0 until computeNTests()).map {
        applyRandomPattern(hopfield)
    }

    suspend fun runTest(hopfield: Layer, nPatterns: Int): Int {
        applyReset()
        applyLearningRate(1.0 / nPatterns)

        val patterns = allPatterns.take(nPatterns)

        patterns.forEach { pattern ->
            hopfield.setActivations(pattern)
            applyTraining()
        }

        return patterns.count { pattern ->
            hopfield.setActivations(pattern)
            workspace.iterateSuspend(2)
            distanceFunction(hopfield.activationArray, pattern) <= patternTester.distancePercentThreshold / 100.0 * hopfield.size
        }
    }

    addButton(buttonName) {

        patternTesterEditor.commitChanges()

        val plot = workspace.getComponent("Memory") as TimeSeriesPlotComponent?
            ?: addTimeSeriesComponent("Memory", seriesNames = listOf("% pattern remembered")).apply {
                model.isAutoRange = false
                model.rangeUpperBound = 105.0
                model.rangeLowerBound = -5.0
            }

        plot.model.clearData()

        for (i in 0 until computeNTests()) {
            val nTest = i + 1
            val nSuccess = runTest(hopfield, nTest) * 100.0 / nTest
            plot.model.addData(0, i.toDouble(), nSuccess.toDouble())
        }


    }

    val nTestChooser = object : EditableObject {
        var nTest by GuiEditable(
            label = "Number of Patterns",
            initValue = 0,
            min = 0,
            max = computeNTests() - 1,
            increment = 1,
            order = 10
        )
    }
    val testChooserEditor = AnnotatedPropertyEditor(nTestChooser)

    addButton("Train Patterns") {
        testChooserEditor.commitChanges()
        runTest(hopfield, nTestChooser.nTest)
    }
    addAnnotatedPropertyEditor(testChooserEditor)


    val nPatternChooser = object : EditableObject {
        var nPattern by GuiEditable(
            label = "Pattern No.",
            initValue = 0,
            min = 0,
            max = computeNTests() - 1,
            increment = 1,
            order = 10
        )
    }
    val patternChooserEditor = AnnotatedPropertyEditor(nPatternChooser)

    addButton("Apply Pattern") {
        patternChooserEditor.commitChanges()
        hopfield.setActivations(allPatterns[nPatternChooser.nPattern])
    }
    addAnnotatedPropertyEditor(patternChooserEditor)
}
