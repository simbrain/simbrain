/** A visual fixed-sequence BPTT task in which a prompt starts a learned output animation. */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import java.awt.Dimension
import javax.swing.JTextArea
import javax.swing.SwingUtilities

val bpttPatternGenerator = newSim {
    workspace.clearWorkspace()
    val component = addNetworkComponent("Pattern generator")
    val network = component.network
    val bptt = BPTTNetwork(PATTERN_COUNT, HIDDEN_UNITS, GRID_SIZE * GRID_SIZE, point(0, 0))
    network.addNetworkModelAsync(bptt)
    bptt.inputLayer.labelArray = arrayOf("zig-zag", "blink", "sweep")
    bptt.inputLayer.circleMode = true
    bptt.outputLayer.gridMode = true
    val expected = NeuronArray(GRID_SIZE * GRID_SIZE).apply {
        label = "Expected"
        gridMode = true
        isClamped = true
        setLocation(bptt.outputLayer.location.x + 350, bptt.outputLayer.location.y)
    }
    network.addNetworkModelAsync(expected, usePlacementManager = false)
    (bptt.inputLayer.updateRule as LinearRule).upperBound = 1.0
    bptt.trainerConfig.sequenceLength = SEQUENCE_LENGTH
    bptt.trainerConfig.learningRate = 0.03
    bptt.updateStateInfoText()
    bptt.unrolledView = false
    bptt.trainingSet = buildPatternGeneratorDataset()

    withGui {
        var step = 0
        var active = false
        var activePattern: Int? = null
        val status = JTextArea().apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            preferredSize = Dimension(270, 42)
            minimumSize = preferredSize
            maximumSize = preferredSize
        }
        fun setStatus(message: String) {
            SwingUtilities.invokeLater { status.text = message }
        }
        fun reset() {
            bptt.resetRecurrentState()
            bptt.inputLayer.setActivations(DoubleArray(PATTERN_COUNT))
            expected.setActivations(DoubleArray(GRID_SIZE * GRID_SIZE))
            step = 0
            active = false
            activePattern = null
            setStatus("Choose a pattern to begin.")
        }
        fun advance(input: DoubleArray) {
            if (step >= SEQUENCE_LENGTH) {
                setStatus("Sequence complete. Reset before continuing.")
                return
            }
            bptt.inputLayer.setActivations(input)
            with(network) { bptt.forwardPass() }
            step++
            activePattern?.let { expected.setActivations(patternFrame(it, step - 1)) }
        }
        fun start(pattern: Int) {
            reset()
            activePattern = pattern
            active = true
            advance(oneHotPattern(PATTERN_COUNT, pattern))
            setStatus("${PATTERN_NAMES[pattern]} — frame $step of $SEQUENCE_LENGTH.")
        }
        fun next() {
            if (!active) setStatus("Choose a pattern first.")
            else {
                advance(DoubleArray(PATTERN_COUNT))
                if (step < SEQUENCE_LENGTH) setStatus("Frame $step of $SEQUENCE_LENGTH.")
            }
        }
        val panel = createControlPanel("Control Panel", 10, 10) {
            PATTERN_NAMES.forEachIndexed { index, name -> addButton(name) { start(index) } }
            addSeparator()
            addButton("Iterate") { next() }
            addButton("Reset") { reset() }
            addSeparator()
            addButton("Show unrolled view") {
                bptt.unrolledView = !bptt.unrolledView
                text = if (bptt.unrolledView) "Hide unrolled view" else "Show unrolled view"
            }
            addComponent(status)
        }.awaitLayout()
        setStatus("Choose a pattern to begin.")
        place(component, panel.rightEdgeWithGap(), 10, 900, 620)
    }
    addSidebarInfo("""
        # Pattern Generator

        A single prompt starts one of three eight-frame animations on the 4×4 output grid. All later input
        frames are blank. The recurrent state therefore has to carry the selected program forward and
        generate its successive output frames. `Expected` shows the target frame beside the network, so it
        is easy to compare it with the learned output.

        Train through the normal network dialog. Then choose a pattern and use `Iterate` to reveal each
        learned frame. Reset before starting another animation.
    """.trimIndent())
}

private const val PATTERN_COUNT = 3
private const val GRID_SIZE = 4
private const val SEQUENCE_LENGTH = 8
private const val HIDDEN_UNITS = 20
private val PATTERN_NAMES = listOf("Row sweep", "Column sweep", "Checker blink")

fun buildPatternGeneratorDataset(): TrainingDataset {
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    repeat(20) {
        repeat(PATTERN_COUNT) { pattern ->
            repeat(SEQUENCE_LENGTH) { step ->
                inputs += if (step == 0) oneHotPattern(PATTERN_COUNT, pattern).toMutableList() else MutableList(PATTERN_COUNT) { 0.0 }
                targets += patternFrame(pattern, step).toMutableList()
            }
        }
    }
    return TrainingDataset(inputs, targets, PATTERN_COUNT, GRID_SIZE * GRID_SIZE)
}

private fun patternFrame(pattern: Int, step: Int): DoubleArray = DoubleArray(GRID_SIZE * GRID_SIZE).also { frame ->
    when (pattern) {
        0 -> (0 until GRID_SIZE).forEach { column -> frame[(step % GRID_SIZE) * GRID_SIZE + column] = 1.0 }
        1 -> (0 until GRID_SIZE).forEach { row -> frame[row * GRID_SIZE + step % GRID_SIZE] = 1.0 }
        else -> frame.indices.filter { index -> (index / GRID_SIZE + index % GRID_SIZE + step) % 2 == 0 }
            .forEach { frame[it] = 1.0 }
    }
}

private fun oneHotPattern(size: Int, index: Int) = DoubleArray(size) { if (it == index) 1.0 else 0.0 }
