/** A variable-delay recall task using fixed-length BPTT training sequences. */
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
import kotlin.random.Random

val bpttRememberSymbol = newSim {
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Remember one symbol")
    val network = networkComponent.network
    val bptt = BPTTNetwork(INPUT_SIZE, HIDDEN_UNITS, SYMBOL_NAMES.size, point(0, 0))
    network.addNetworkModelAsync(bptt)
    bptt.inputLayer.labelArray = (SYMBOL_NAMES + "recall").toTypedArray()
    bptt.outputLayer.labelArray = SYMBOL_NAMES.toTypedArray()
    bptt.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }
    (bptt.inputLayer.updateRule as LinearRule).upperBound = 1.0
    bptt.trainerConfig.sequenceLength = SEQUENCE_LENGTH
    bptt.trainerConfig.learningRate = LEARNING_RATE
    bptt.updateStateInfoText()
    bptt.unrolledView = false
    bptt.trainingSet = buildVariableDelayRecallDataset(TRAINING_REPETITIONS, Random(TRAINING_SEED))

    withGui {
        var step = 0
        var active = false
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
            bptt.inputLayer.setActivations(DoubleArray(INPUT_SIZE))
            step = 0
            active = false
            setStatus("Choose A, B, C, or D.")
        }
        fun advance(input: DoubleArray): Boolean {
            if (step >= SEQUENCE_LENGTH) {
                setStatus("Trial complete. Reset before continuing.")
                return false
            }
            bptt.inputLayer.setActivations(input)
            with(network) { bptt.forwardPass() }
            step++
            return true
        }
        fun cue(symbol: Int) {
            reset()
            active = advance(oneHot(INPUT_SIZE, symbol).toDoubleArray())
            setStatus("${SYMBOL_NAMES[symbol]} stored — step $step of $SEQUENCE_LENGTH.")
        }
        fun blank() {
            if (!active) setStatus("Choose a symbol first.")
            else if (advance(DoubleArray(INPUT_SIZE)) && step < SEQUENCE_LENGTH) setStatus("Blank step $step of $SEQUENCE_LENGTH.")
        }
        fun recall() {
            if (!active) setStatus("Choose a symbol first.")
            else if (advance(oneHot(INPUT_SIZE, SYMBOL_NAMES.size).toDoubleArray())) setStatus("Recall at step $step of $SEQUENCE_LENGTH.")
        }
        val panel = createControlPanel("Control Panel", 10, 10) {
            SYMBOL_NAMES.forEachIndexed { index, symbol -> addButton(symbol) { cue(index) } }
            addSeparator()
            addButton("Iterate") { blank() }
            addButton("Recall") { recall() }
            addButton("Reset") { reset() }
            addSeparator()
            addButton("Show unrolled view") {
                bptt.unrolledView = !bptt.unrolledView
                text = if (bptt.unrolledView) "Hide unrolled view" else "Show unrolled view"
            }
            addComponent(status)
        }.awaitLayout()
        setStatus("Choose A, B, C, or D.")
        place(networkComponent, panel.rightEdgeWithGap(), 12, 900, 620)
    }
    addSidebarInfo("""
        # Remember One Symbol

        Every shaded band in the training table is one $SEQUENCE_LENGTH-step trial. A, B, C, or D is shown
        first; the recall cue occurs at a different later step in every kind of example. The remaining rows
        pad the trial so that every BPTT computation has the same length.

        ## What to Do

        1. Double-click the network and train the supplied data in the normal training dialog.
        2. Click A, B, C, or D, then click `Iterate` any number of times.
        3. Click `Recall`. The output should show the stored symbol.
        4. Reset before another trial. The model is trained for no more than $SEQUENCE_LENGTH steps.
    """.trimIndent())
}

private const val SEQUENCE_LENGTH = 7
private const val INPUT_SIZE = 5
private const val HIDDEN_UNITS = 8
private const val TRAINING_REPETITIONS = 12
private const val LEARNING_RATE = 0.01
private const val TRAINING_SEED = 1
private val SYMBOL_NAMES = listOf("A", "B", "C", "D")

fun buildVariableDelayRecallDataset(repetitions: Int, random: Random): TrainingDataset {
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    repeat(repetitions) {
        SYMBOL_NAMES.indices.shuffled(random).forEach { symbol ->
            (1 until SEQUENCE_LENGTH).shuffled(random).forEach { recallStep ->
                repeat(SEQUENCE_LENGTH) { step ->
                    inputs += when (step) {
                        0 -> oneHot(INPUT_SIZE, symbol)
                        recallStep -> oneHot(INPUT_SIZE, SYMBOL_NAMES.size)
                        else -> zeros(INPUT_SIZE)
                    }
                    targets += if (step == recallStep) oneHot(SYMBOL_NAMES.size, symbol) else zeros(SYMBOL_NAMES.size)
                }
            }
        }
    }
    return TrainingDataset(inputs, targets, INPUT_SIZE, SYMBOL_NAMES.size)
}

private fun oneHot(size: Int, index: Int) = MutableList(size) { if (it == index) 1.0 else 0.0 }
private fun zeros(size: Int) = MutableList(size) { 0.0 }
