package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.network.core.Layer
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.workspace.Workspace
import java.util.*
import javax.swing.JLabel
import javax.swing.JSlider
import kotlin.math.abs

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

fun applyRandomPattern(hopfield: Layer): DoubleArray {
    hopfield.randomize(TwoValued(-1.0, 1.0))
    return hopfield.activationArray
}

class HopfieldTestConfig(
    val workspace: Workspace,
    val hopfield: Layer,
    val patternTestConfig: PatternTestConfig,
    val applyTraining: suspend () -> Unit,
    val applyLearningRate: (learningRate: Double) -> Unit,
    val applyReset: () -> Unit,
    val distanceFunction: (actual: DoubleArray, expected: DoubleArray) -> Double = ::hammingDistance)

suspend fun runHopfieldTest(config: HopfieldTestConfig, patterns: List<DoubleArray>, numPatternsToTest: Int): Int {
    config.applyReset()
    config.applyLearningRate(1.0 / numPatternsToTest)

    val patterns = patterns.take(numPatternsToTest)
    patterns.forEach { pattern ->
        config.hopfield.setActivations(pattern)
        config.applyTraining()
    }

    // Returns the number of patterns that remain stable within the specified tolerance
    return patterns.count { pattern ->
        config.hopfield.setActivations(pattern)
        config.workspace.iterateSuspend(2)
        config.distanceFunction(
            config.hopfield.activationArray,
            pattern
        ) <= config.patternTestConfig.distancePercentThreshold / 100.0 * config.hopfield.size
    }
}

suspend fun runCapacityTest(
    config: HopfieldTestConfig,
    patterns: List<DoubleArray>,
    numPatternsToTest: Int,
    plot: TimeSeriesPlotComponent
) {

    // Runs the memory test for 1, 2, ... numTestPatterns and plots results
    for (i in 0 until numPatternsToTest) {
        val nTest = i + 1
        val nSuccess = runHopfieldTest(config, patterns, nTest) * 100.0 / nTest
        plot.model.addData(0, i.toDouble(), nSuccess)
    }

}

context(SimulationScope)
fun ControlPanelKt.createHopfieldTestPane(
    config: HopfieldTestConfig
) {

    val patternTestConfig = PatternTestConfig()
    fun numTestPatterns(): Int = (patternTestConfig.percentToTest / 100 * config.hopfield.size).toInt()
    val allPatterns = (0 until numTestPatterns()).map {
        applyRandomPattern(config.hopfield)
    }

    addTab("Capacity")

    // Slider for patterns
    val slider = JSlider(0, numTestPatterns() - 1, 0)
    fun JSlider.init() {
        minorTickSpacing = 1
        val labelTable = Hashtable<Int, JLabel>()
        labelTable[minimum] = JLabel("${minimum + 1}")
        labelTable[maximum] = JLabel("${maximum + 1}")
        labelTable[(minimum + maximum) / 2] = JLabel("${((minimum + maximum) / 2) + 1}")
        paintTicks = true
        paintLabels = true
        snapToTicks = true
        setLabelTable(labelTable)
    }
    slider.init()

    addButton("Capacity Test", tab = "Capacity") {
        patternTestConfig.showAPEOptionDialog("Capacity Test Parameters")
        val plot = workspace.getComponent("Memory") as TimeSeriesPlotComponent?
            ?: addTimeSeriesComponent("Memory", seriesNames = listOf("% pattern remembered")).apply {
                model.isAutoRange = false
                model.rangeUpperBound = 105.0
                model.rangeLowerBound = -5.0
            }

        // Reset slider
        slider.maximum = numTestPatterns() - 1
        slider.init()

        plot.model.clearData()
        runCapacityTest(config, allPatterns, numTestPatterns(), plot)

    }

    val patternNum = JLabel("   Pattern number: ")
    addComponent(slider,  tab = "Capacity")
    slider.addChangeListener {
        config.hopfield.setActivations(allPatterns[slider.value])
        patternNum.text = "   Pattern number: ${slider.value + 1}"
    }
    addComponent(patternNum, tab = "Capacity")

    //val nTestChooser = object : EditableObject {
    //    var nTest by GuiEditable(
    //        label = "Number of Patterns",
    //        initValue = 0,
    //        min = 0,
    //        max = numTestPatterns() - 1,
    //        increment = 1,
    //        order = 10
    //    )
    //}
    //val testChooserEditor = AnnotatedPropertyEditor(nTestChooser)
    //addButton("Train Patterns", tab = "Capacity") {
    //    testChooserEditor.commitChanges()
    //    runTest(hopfield, nTestChooser.nTest)
    //}
    //addAnnotatedPropertyEditor(testChooserEditor, tab = "Capacity")

    val nPatternChooser = object : EditableObject {
        var nPattern by GuiEditable(
            label = "Pattern No.",
            initValue = 0,
            min = 0,
            max = numTestPatterns() - 1,
            increment = 1,
            order = 10
        )
    }
    val patternChooserEditor = AnnotatedPropertyEditor(nPatternChooser)
    //addSeparator(tab = "Capacity")
    //addAnnotatedPropertyEditor(patternChooserEditor, tab = "Capacity")
    //addButton("Load Pattern", tab = "Capacity") {
    //    patternChooserEditor.commitChanges()
    //    hopfield.setActivations(allPatterns[nPatternChooser.nPattern])
    //}

}

class PatternTestConfig: EditableObject {

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