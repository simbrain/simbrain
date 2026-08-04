/**
 * A memory task that cannot be learned unless the gradient reaches back far enough to the moment worth
 * remembering.
 *
 * A symbol is shown, then nothing for several steps, then a recall cue. Answering correctly means the
 * recurrent weights have to be shaped to carry the symbol across the gap, and they can only be shaped that
 * way if the error at the recall step has a gradient path back to the step that saw the symbol. Truncation
 * depth is the length of that path, which is what makes it the one knob here.
 *
 * The task's parameters, sequence builder, and scorer sit outside the simulation body rather than inside it
 * because the simulation's central claim is worth testing, and a test has to be able to reach them.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.format
import org.simbrain.util.place
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.ALPHABET_SIZE
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.DELAY
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.HIDDEN_UNITS
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.LEARNING_RATE
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.TRAINING_TRIALS
import org.simbrain.custom_sims.simulations.demos.DelayedRecall.TRIAL_LENGTH
import org.simbrain.util.point
import kotlin.random.Random

val bpttDelayedRecall = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Delayed recall")
    val network = networkComponent.network

    val bptt = BPTTNetwork(ALPHABET_SIZE + 1, HIDDEN_UNITS, ALPHABET_SIZE, point(0, 0))
    network.addNetworkModelAsync(bptt)

    bptt.inputLayer.labelArray = (SYMBOL_NAMES + "recall").toTypedArray()
    bptt.outputLayer.labelArray = SYMBOL_NAMES.toTypedArray()
    bptt.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }

    bptt.trainingSet = buildDelayedRecallSequence(TRAINING_TRIALS, Random(TRAINING_SEED))
    bptt.trainerConfig.truncationDepth = TRIAL_LENGTH
    bptt.trainerConfig.learningRate = LEARNING_RATE
    bptt.updateStateInfoText()
    bptt.unrolledView = true

    val trainer = BPTTTrainer(network, bptt)
    val testSet = buildDelayedRecallSequence(TEST_TRIALS, Random(TEST_SEED))

    withGui {
        val timeSeries = addTimeSeriesComponent("Training error", "error")

        createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {

            val accuracyText = addLabelledText("Recall accuracy: ", "not yet tested")
            val errorText = addLabelledText("Training error: ", "not yet trained")
            val reachText = addLabelledText("Gradient reach: ", describeReach(TRIAL_LENGTH))

            fun test() = with(network) { bptt.measureRecallAccuracy(testSet) }

            addSlider(
                "Truncation depth",
                minValue = 1.0,
                maxValue = MAX_DEPTH.toDouble(),
                initValue = TRIAL_LENGTH.toDouble(),
                increment = 1.0,
                toolTip = "How many steps back the gradient reaches. A trial is $TRIAL_LENGTH steps long."
            ) { depth ->
                bptt.trainerConfig.truncationDepth = depth.toInt()
                bptt.updateStateInfoText()
                reachText.text = describeReach(depth.toInt())
            }

            addButton("Train $EPOCHS_PER_CLICK epochs") {
                repeat(EPOCHS_PER_CLICK) {
                    trainer.trainOnce()
                    timeSeries.model.timeSeriesList[0].series.add(trainer.iteration, trainer.lastTrainingError)
                }
                errorText.text = trainer.lastTrainingError.format(4)
                accuracyText.text = test().asPercent()
            }

            addButton("Test recall") {
                accuracyText.text = test().asPercent()
            }

            addButton("Reset weights") {
                trainer.randomize()
                trainer.iteration = 0
                timeSeries.model.timeSeriesList[0].series.clear()
                errorText.text = "not yet trained"
                accuracyText.text = "not yet tested"
            }
        }

        place(networkComponent, SIM_WINDOW_GAP, 280, 900, 620)
        place(timeSeries, SIM_WINDOW_GAP + 900 + SIM_WINDOW_GAP, 280, 450, 620)
    }

    addSidebarInfo(
        """
        # Delayed Recall

        A network is shown one of $ALPHABET_SIZE symbols, then nothing at all for $DELAY steps, then a
        recall cue. It has to answer with the symbol it saw before the gap.

        Nothing in the input at the moment of recall says which symbol it was, so the answer can only come
        from what the network has been carrying in its hidden layer across the gap. That is what makes this
        a test of memory rather than of mapping.

        # Simulation Details

        Carrying information forward and learning to carry it are different problems. A recurrent network's
        hidden state passes something forward however it is trained, and random recurrent weights will leak
        a little of the symbol across the gap by accident. Learning to carry it deliberately is another
        matter: the error appears at the recall step, but the weights that had to encode the symbol were
        used ${DELAY + 1} steps earlier. Improving those weights requires a gradient that reaches back that
        far.

        Truncation depth is that reach. Backpropagation through time unrolls the network over a window of
        that many steps and stops there. One trial is $TRIAL_LENGTH steps: a cue, $DELAY blanks, and a
        recall. A depth of $TRIAL_LENGTH lets the gradient from a recall reach the cue it answers; anything
        less cuts the path somewhere in the middle.

        ## Control Panel Settings

        - `Truncation depth` sets how many steps the gradient reaches back, and `Gradient reach` reports
          whether that is enough to connect a recall to its cue.
        - `Train` runs $EPOCHS_PER_CLICK epochs over the whole sequence and then tests.
        - `Test recall` scores only the recall steps. The cue and delay steps share an all-zero target, so a
          network that had learned nothing but to stay quiet would otherwise look accurate.
        - `Reset weights` randomizes and clears the error plot.

        Chance is ${(100.0 / ALPHABET_SIZE).format(0)}%, since there are $ALPHABET_SIZE symbols to choose
        between.

        # What to Do

        1. Leave `Truncation depth` at $TRIAL_LENGTH and click `Train`. Recall accuracy reaches 100%.

        2. Click `Reset weights`, drag `Truncation depth` down to `2`, and train just as much. Accuracy sits
           at chance and stays there however many times you click `Train`. No amount of training helps,
           because there is no gradient path from a recall back to the cue it answers, so the recurrent
           weights are never shaped to preserve anything. Depths of `1` and `3` behave the same way.

        3. At any depth below $TRIAL_LENGTH, watch the `Output` layer instead of the accuracy figure. The
           same unit lights up on every recall whatever symbol was shown: the network settles on one
           constant answer and scores whatever share of the data that symbol happens to hold. A figure
           somewhere between chance and correct looks like partial memory and is not; nothing is being
           recalled at all. Only at $TRIAL_LENGTH does the answer start varying with the cue.

        4. Try `6` or `7`, more than a trial. These still solve the task, but slightly less dependably than
           $TRIAL_LENGTH does, and the reason is worth a moment. Truncation here is by fixed chunks rather
           than a sliding window: windows always start at the first row and run the depth's length. At a
           depth of $TRIAL_LENGTH every window is exactly one trial, and at any other depth the windows
           drift out of step with the trials, so some trials get split across a boundary and lose the path
           from their recall to their cue. Reach that lines up with the structure of the data beats reach
           for its own sake.

        ## Watching it happen

        The network is drawn unrolled, so the columns to the left of the live network are the preceding
        steps, and there are as many of them as the truncation depth. Moving the slider changes the picture,
        which is the most direct way to see what the setting means.

        Double-click the network to open the training dialog. Its rows are timesteps and the shaded bands
        are the truncation windows. At a depth of $TRIAL_LENGTH every band is exactly one trial, so each cue
        and its recall fall inside the same band. Change the depth and watch the bands stop lining up with
        the trials, which is the same fact the accuracy is reporting.

        # References

        The task is a construction for this simulation, not a reproduction of a published experiment,
        though delaying a cue and then probing for it is a long established paradigm. The reference below
        is for the training algorithm rather than for the task.

        Werbos, P. J. (1990). [*Backpropagation through time: what it does and how to
        do it.*](https://doi.org/10.1109/5.58337) Proceedings of the IEEE, 78(10), 1550-1560.
        """.trimIndent()
    )

}

/**
 * Grouped into an object rather than left as top-level constants because names this generic would sit in
 * the same package as every other demo simulation.
 */
object DelayedRecall {
    const val ALPHABET_SIZE = 3
    const val DELAY = 3

    /** A cue step, [DELAY] blank steps, then a recall step. */
    const val TRIAL_LENGTH = DELAY + 2

    /**
     * Small on purpose. With more units the recurrent weights are likely enough to preserve the symbol by
     * accident that a window too short to learn anything still scores well above chance, which blunts the
     * whole comparison.
     */
    const val HIDDEN_UNITS = 4
    const val TRAINING_TRIALS = 30

    /** Higher rates reach chance performance and stay there; this one solves the task on every seed tried. */
    const val LEARNING_RATE = 0.01
}

private const val TEST_TRIALS = 60
private const val EPOCHS_PER_CLICK = 400
private const val MAX_DEPTH = 8
private const val TRAINING_SEED = 1
private const val TEST_SEED = 2

private val SYMBOL_NAMES = listOf("A", "B", "C")

/**
 * One symbol per trial, laid out as a single continuous sequence.
 *
 * Every step needs a target because the loss is summed over the whole unrolled window, so the delay steps
 * are given an all-zero target rather than being masked out. The network therefore also has to learn to
 * stay silent until asked, which is a fair part of the task rather than an artifact of the encoding.
 */
fun buildDelayedRecallSequence(trials: Int, random: Random): TrainingDataset {
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    val silence = { MutableList(ALPHABET_SIZE) { 0.0 } }
    repeat(trials) {
        val symbol = random.nextInt(ALPHABET_SIZE)
        inputs.add(oneHot(ALPHABET_SIZE + 1, symbol))
        targets.add(silence())
        repeat(DELAY) {
            inputs.add(MutableList(ALPHABET_SIZE + 1) { 0.0 })
            targets.add(silence())
        }
        // The recall cue is its own input unit, so nothing in the input at this step identifies the symbol.
        inputs.add(oneHot(ALPHABET_SIZE + 1, ALPHABET_SIZE))
        targets.add(oneHot(ALPHABET_SIZE, symbol))
    }
    return TrainingDataset(inputs, targets, ALPHABET_SIZE + 1, ALPHABET_SIZE)
}

/**
 * Fraction of recall steps answered with the right symbol, running [testSet] through as one continuous
 * sequence so each trial is answered from memory built up the way it would be in use.
 */
context(Network)
fun BPTTNetwork.measureRecallAccuracy(testSet: TrainingDataset): Double {
    var correct = 0
    resetRecurrentState()
    testSet.inputs.forEachIndexed { row, input ->
        inputLayer.setActivations(input.toDoubleArray())
        forwardPass()
        if (row % TRIAL_LENGTH == TRIAL_LENGTH - 1) {
            if (outputLayer.activationArray.indexOfMax() == testSet.targets[row].indexOfMax()) correct++
        }
    }
    return correct.toDouble() / (testSet.inputs.size / TRIAL_LENGTH)
}

private fun oneHot(size: Int, index: Int) = MutableList(size) { if (it == index) 1.0 else 0.0 }

private fun DoubleArray.indexOfMax() = withIndex().maxBy { it.value }.index

private fun List<Double>.indexOfMax() = withIndex().maxBy { it.value }.index

private fun Double.asPercent() = "${(this * 100).format(1)}%"

private fun describeReach(depth: Int) = if (depth >= TRIAL_LENGTH) {
    "reaches the cue ($depth of $TRIAL_LENGTH steps)"
} else {
    "stops short of the cue ($depth of $TRIAL_LENGTH steps)"
}
