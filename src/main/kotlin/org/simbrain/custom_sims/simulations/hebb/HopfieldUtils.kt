package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.network.core.Layer
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.applyFunctionInPlace
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.Workspace
import java.util.*
import javax.swing.JLabel
import javax.swing.JSlider

fun hammingDistance(actual: DoubleArray, expected: DoubleArray): Double {
    return actual.zip(expected).count { (a, b) -> a != b }.toDouble()
}

/**
 * Returns the number of entries that have opposite signs
 */
fun signedHammingDistance(actual: DoubleArray, expected: DoubleArray): Double {
    return actual.zip(expected).sumOf { (a, b) ->
        val sameSign = a * b >= 0
        return@sumOf if (sameSign) 0.0 else 1.0
    }
}

fun applyRandomPattern(hopfield: Layer): DoubleArray {
    hopfield.randomize(TwoValued(-1.0, 1.0))
    return hopfield.activationArray
}

/***
 * For the specified number of patterns, train the network a specified number of times, then apply
 * forgetting the specified number of times, then determine how many patterns were learned up to a
 * specified tolerance.
 */
suspend fun forgettingTest(
    config: HopfieldTestConfig,
    numPatterns: Int,
    iterationsToForget: Int,
    tolerance: Double,
    testIterations: Int
): Int {
    config.weights.weights.randomizeSymmetric()
    val patterns = (0 until numPatterns).map {
        applyRandomPattern(config.hopfield)
    }

    // Training
    patterns.forEach { pattern ->
        config.hopfield.setActivations(pattern)
        config.applyTraining()
    }

    // Forgetting
    repeat(iterationsToForget) {
        if (config.weights.learningRule is HebbianRule) {
            (config.weights.learningRule as HebbianRule).applyForgetting(config.weights)
        } else {
            config.weights.weights.mul(.9)
        }
    }

    // Test recall
    return patterns.count { pattern ->
        config.hopfield.setActivations(pattern)
        config.workspace.iterateSuspend(testIterations)
        val distance = config.distanceFunction(
            config.hopfield.activationArray,
            pattern
        )
        distance <= tolerance
    }

}

data class HopfieldTestConfig(
    val workspace: Workspace,
    val hopfield: Layer,
    val weights: WeightMatrix,
    val applyTraining: suspend () -> Unit,
    val applyLearningRate: (learningRate: Double) -> Unit,
    val applyReset: () -> Unit,
    val distanceFunction: (actual: DoubleArray, expected: DoubleArray) -> Double = ::hammingDistance
)

suspend fun runHopfieldTest(config: HopfieldTestConfig, patternTestConfig: PatternTestOptions, patterns: List<DoubleArray>, numPatternsToTest: Int): Int {
    config.applyReset()
    config.applyLearningRate(1.0 / numPatternsToTest)

    if (patternTestConfig.forgetting) {
        (config.weights.learningRule as? HebbianRule)?.let { hebbianRule ->
            hebbianRule.forgettingRate = patternTestConfig.forgettingRate
        }
    }

    val patterns = patterns.take(numPatternsToTest)
    patterns.forEach { pattern ->
        config.hopfield.setActivations(pattern)
        config.applyTraining()
    }

    if (patternTestConfig.forgetting) {
        repeat(patternTestConfig.forgettingIterations) {
            if (config.weights.learningRule is HebbianRule) {
                (config.weights.learningRule as HebbianRule).applyForgetting(config.weights)
            } else {
                config.weights.weights.mul(1 - patternTestConfig.forgettingRate)
            }
            if (patternTestConfig.perturbWeights) {
                config.weights.weights.applyFunctionInPlace { w -> w + patternTestConfig.perturbFunction.sampleDouble() }
                config.weights.events.updated.fire()
            }
        }
    }


    // Returns the number of patterns that remain stable within the specified tolerance
    return patterns.count { pattern ->
        config.hopfield.setActivations(pattern)
        config.workspace.iterateSuspend(patternTestConfig.testIterations)
        config.distanceFunction(
            config.hopfield.activationArray,
            pattern
        ) <= patternTestConfig.distancePercentThreshold / 100.0 * config.hopfield.size
    }
}

suspend fun runCapacityTest(
    config: HopfieldTestConfig,
    patternTestConfig: PatternTestOptions,
    patterns: List<DoubleArray>,
    numPatternsToTest: Int,
    plot: TimeSeriesPlotComponent
) {

    // Runs the memory test for 1, 2, ... numTestPatterns and plots results
    for (i in 0 until numPatternsToTest) {
        val nTest = i + 1
        val nSuccess = runHopfieldTest(config, patternTestConfig.copy().apply { forgetting = false }, patterns, nTest) * 100.0 / nTest
        plot.model.addData(0, i.toDouble(), nSuccess)
    }

    if (patternTestConfig.forgetting) {
        for (i in 0 until numPatternsToTest) {
            val nTest = i + 1
            val nSuccess = runHopfieldTest(config, patternTestConfig, patterns, nTest) * 100.0 / nTest
            plot.model.addData(1, i.toDouble(), nSuccess)
        }
    }

}

context(SimulationScope)
fun ControlPanelKt.createHopfieldTestPane(
    config: HopfieldTestConfig
) {

    val patternTestConfig = PatternTestOptions()
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
        val seriesNames = buildList {
            add("% pattern remembered")
            if (patternTestConfig.forgetting) add("% pattern remembered (with forgetting)")
        }
        val plot = workspace.getComponent("Memory") as TimeSeriesPlotComponent?
            ?: addTimeSeriesComponent("Memory", seriesNames).apply {
                model.isAutoRange = false
                model.rangeUpperBound = 105.0
                model.rangeLowerBound = -5.0
            }

        // Reset slider
        slider.maximum = numTestPatterns() - 1
        slider.init()

        plot.model.clearData()
        runCapacityTest(config, patternTestConfig, allPatterns, numTestPatterns(), plot)

    }

    val patternNum = JLabel("   Pattern number: ")
    addComponent(slider, tab = "Capacity")
    slider.addChangeListener {
        config.hopfield.setActivations(allPatterns[slider.value])
        patternNum.text = "   Pattern number: ${slider.value + 1}"
    }
    addComponent(patternNum, tab = "Capacity")

}

class PatternTestOptions: CopyableObject {

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

    var testIterations by GuiEditable(
        initValue = 5,
        description = "Number of times to iterate when testing recalled pattern",
        order = 30
    )

    var forgetting by GuiEditable(
        initValue = false,
        description = "Enable forgetting",
        order = 40
    )

    var forgettingIterations by GuiEditable(
        initValue = 10,
        description = "Number of iterations to apply forgetting",
        order = 50,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var forgettingRate by GuiEditable(
        initValue = 0.1,
        description = "Forgetting rate",
        order = 60,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var perturbWeights by GuiEditable(
        initValue = false,
        description = "Perturb weights",
        order = 70,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var perturbFunction by GuiEditable(
        initValue = UniformRealDistribution(-0.1, 0.1) as ProbabilityDistribution,
        description = "Perturb function",
        order = 80,
        conditionallyVisibleBy = PatternTestOptions::perturbWeights
    )

    override fun copy(): PatternTestOptions {
        return PatternTestOptions().also {
            it.distancePercentThreshold = distancePercentThreshold
            it.percentToTest = percentToTest
            it.testIterations = testIterations
            it.forgetting = forgetting
            it.forgettingIterations = forgettingIterations
            it.forgettingRate = forgettingRate
            it.perturbWeights = perturbWeights
            it.perturbFunction = perturbFunction.copy()
        }
    }

}

class ForgettingTestOptions: EditableObject {

    var numPatterns by GuiEditable(
        initValue = 10,
        description = "Number of patterns to test",
        order = 10,
    )

    var iterationsToForget by GuiEditable(
        initValue = 10,
        description = "Number of iterations to apply forgetting",
        order = 30,
    )

    var tolerance by GuiEditable(
        initValue = 5.0,
        description = "Number of nodes that can be different and the pattern considered the same",
        order = 40
    )

    var testIterations by GuiEditable(
        initValue = 10,
        description = "Number of times to iterate when testing recalled pattern",
        order = 50
    )

}