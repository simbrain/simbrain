package org.simbrain.custom_sims.simulations.hebb

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.network.core.Layer
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
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

data class HopfieldTestConfig(
    val workspace: Workspace,
    val hopfield: Layer,
    val weights: WeightMatrix,
    val applyTraining: suspend () -> Unit,
    val applyLearningRate: (learningRate: Double) -> Unit,
    val applyReset: () -> Unit,
    val distanceFunction: (actual: DoubleArray, expected: DoubleArray) -> Double = ::hammingDistance
)

suspend fun testPatterns(
    config: HopfieldTestConfig,
    patternTestConfig: PatternTestOptions,
    patterns: List<DoubleArray>,
    numPatternsToTest: Int
): Int {

    config.applyReset()
    config.applyLearningRate(1.0 / numPatternsToTest)

    // Set forgetting rates if needed
    if (patternTestConfig.forgetting) {
        (config.weights.learningRule as? HebbianRule)?.let { hebbianRule ->
            hebbianRule.forgettingRate = patternTestConfig.decayRate
        }
    }

    // Set up and learn patterns
    val patterns = patterns.take(numPatternsToTest)
    patterns.forEach { pattern ->
        config.hopfield.setActivations(pattern)
        config.applyTraining()
    }

    // Forgetting test
    if (patternTestConfig.forgetting) {
        repeat(patternTestConfig.forgettingIterations) {
            if (config.weights.learningRule is HebbianRule) {
                (config.weights.learningRule as HebbianRule).applyForgetting(config.weights)
            } else {
                config.weights.weights.mul(1 - patternTestConfig.decayRate)
            }
            if (patternTestConfig.perturbWeights) {
                config.weights.weights.applyFunctionInPlace { w -> w + patternTestConfig.perturbFunction.sampleDouble() }
                config.weights.events.updated.fire()
            }
        }
    }

    // Returns the number of patterns that remain stable within the specified tolerance
    return patterns.count { pattern ->
        // Apply partial cue of pattern to network
        if (patternTestConfig.isDiscreteHopfield) {
            config.hopfield.setActivations(pattern.perturbBinaryByHammingDistance(patternTestConfig.cueDistance.toInt()))
        } else {
            config.hopfield.setActivations(pattern.perturbByEuclideanDistance(patternTestConfig.cueDistance))
        }
        // Run for specified iterations
        config.workspace.iterateSuspend(patternTestConfig.testIterations)
        // Test if it's within a specified radius of the original pattern
        config.distanceFunction(
            config.hopfield.activationArray,
            pattern
        ) <= patternTestConfig.distancePercentThreshold / 100.0 * config.hopfield.size
    }
}

/**
 * Run tests for memory capacity with and without forgetting
 */
suspend fun runCapacityTests(
    config: HopfieldTestConfig,
    patternTestConfig: PatternTestOptions,
    patterns: List<DoubleArray>,
    numPatternsToTest: Int,
    plot: TimeSeriesPlotComponent
) {

    // Runs the memory test and plot results
    for (i in 0 until numPatternsToTest) {
        val nTest = i + 1
        val nSuccess = testPatterns(config, patternTestConfig.copy().apply { forgetting = false }, patterns, nTest) * 100.0 / nTest
        plot.model.addData(0, (i+1).toDouble(), nSuccess)
    }

    // Run the forgetting test and plot results
    if (patternTestConfig.forgetting) {
        for (i in 0 until numPatternsToTest) {
            val nTest = i + 1
            val nSuccess = testPatterns(config, patternTestConfig, patterns, nTest) * 100.0 / nTest
            plot.model.addData(1, (i+1).toDouble(), nSuccess)
        }
    }

}

context(SimulationScope)
fun ControlPanelKt.createHopfieldTestPane(
    config: HopfieldTestConfig,
    isDiscreteHopfield: Boolean
) {

    val patternTestConfig = PatternTestOptions(isDiscreteHopfield)
    fun numTestPatterns(): Int = (patternTestConfig.percentToTest / 100 * config.hopfield.size).toInt()

    // When the panel is created a specific set of patterns is created so that runs of all tests for any percentage of patterns
    // should produce determinate results
    val allPatterns = (0 until   config.hopfield.size).map {
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
        runCapacityTests(config, patternTestConfig, allPatterns, numTestPatterns(), plot)

    }

    val patternNum = JLabel("   Pattern number: ")
    addComponent(slider, tab = "Capacity")
    slider.addChangeListener {
        config.hopfield.setActivations(allPatterns[slider.value])
        patternNum.text = "   Pattern number: ${slider.value + 1}"
    }
    addComponent(patternNum, tab = "Capacity")

}

class PatternTestOptions(val isDiscreteHopfield: Boolean): CopyableObject {

    var distancePercentThreshold by GuiEditable(
        label = "Distance Threshold",
        description = "Max allowable distance (for discrete Hopfield, Hamming distance) between the recalled and original pattern to be considered correctly remembered",
        initValue = 5.0,
        min = 0.0,
        max = 100.0,
        increment = 1.0,
        order = 10
    )

    var percentToTest by GuiEditable(
        label = "Percent to Test",
        description = "Percent of total stored patterns that will be tested for recall.",
        initValue = 30.0,
        min = 0.0,
        max = 100.0,
        order = 20
    )

    var cueDistance by GuiEditable(
        initValue = 3.0,
        description = "How far away the cue should be when testing retrieval. Hamming for discrete, Euclidean for continuous.",
        min = 0.0,
        increment = 1.0,
        order = 25
    )

    var testIterations by GuiEditable(
        initValue = 5,
        description = "Number of iterations (proxy for time) the network will run when testing recall of stored pattern.",
        order = 30
    )

    var forgetting by GuiEditable(
        initValue = false,
        description = "If true, enable forgetting",
        order = 40
    )

    var forgettingIterations by GuiEditable(
        initValue = 10,
        description = "Number of times the forgetting process (scaling, perturbation, or both) is applied",
        order = 50,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var decayRate by GuiEditable(
        initValue = 0.1,
        description = "Rate that weights are decayed during forgetting. For example, if set to .25, then a weight of " +
                "10 would be reduced by 2.5 in the next iteration. Set to 0 to disable this type of forgetting.",
        order = 60,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var perturbWeights by GuiEditable(
        initValue = false,
        description = "If true, perturb weights during forgetting, by applying a number to each weight supplied by" +
                "a probability distribution.",
        order = 70,
        conditionallyVisibleBy = PatternTestOptions::forgetting
    )

    var perturbFunction by GuiEditable(
        initValue = UniformRealDistribution(-0.1, 0.1) as ProbabilityDistribution,
        description = "The statistical distribution used for perturbing the weights during forgetting.",
        order = 80,
        onUpdate = {
            showWidget(widgetValue(::forgetting) && widgetValue(::perturbWeights))
        }
    )

    override fun copy(): PatternTestOptions {
        return PatternTestOptions(isDiscreteHopfield).also {
            it.distancePercentThreshold = distancePercentThreshold
            it.percentToTest = percentToTest
            it.cueDistance = cueDistance
            it.testIterations = testIterations
            it.forgetting = forgetting
            it.forgettingIterations = forgettingIterations
            it.decayRate = decayRate
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