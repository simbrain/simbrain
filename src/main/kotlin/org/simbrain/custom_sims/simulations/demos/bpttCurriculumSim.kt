/**
 * Two training sets of identical size, differing only in the range of memory spans they contain, trained
 * at a truncation depth far too shallow for the long ones.
 *
 * The set containing short spans as well as long ones solves every span. The set containing only the long
 * ones solves none of them, despite offering more examples of exactly the spans being tested. What the
 * short spans teach is a rule for carrying a symbol one step, and that rule composes: applied repeatedly
 * it carries the symbol any distance, without the gradient ever having spanned that distance.
 *
 * Sibling of [bpttDelayedRecall], which holds the data fixed and varies the depth. This one holds the
 * depth fixed and varies the data, and between them they separate what truncation depth limits from what
 * it does not.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.addTimeSeriesComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.demos.Curriculum.ALPHABET_SIZE
import org.simbrain.custom_sims.simulations.demos.Curriculum.GAPS
import org.simbrain.custom_sims.simulations.demos.Curriculum.HIDDEN_UNITS
import org.simbrain.custom_sims.simulations.demos.Curriculum.LEARNING_RATE
import org.simbrain.custom_sims.simulations.demos.Curriculum.LONG_GAPS
import org.simbrain.custom_sims.simulations.demos.Curriculum.TRIAL_LENGTH
import org.simbrain.custom_sims.simulations.demos.Curriculum.TRUNCATION_DEPTH
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.format
import org.simbrain.util.place
import org.simbrain.util.point
import kotlin.random.Random

val bpttCurriculum = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Curriculum")
    val network = networkComponent.network

    val bptt = BPTTNetwork(ALPHABET_SIZE + 1, HIDDEN_UNITS, ALPHABET_SIZE, point(0, 0))
    network.addNetworkModelAsync(bptt)

    bptt.inputLayer.labelArray = (SYMBOL_NAMES + "recall").toTypedArray()
    bptt.outputLayer.labelArray = SYMBOL_NAMES.toTypedArray()
    bptt.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }

    bptt.trainerConfig.truncationDepth = TRUNCATION_DEPTH
    bptt.trainerConfig.learningRate = LEARNING_RATE
    bptt.updateStateInfoText()
    bptt.unrolledView = true

    val trainer = BPTTTrainer(network, bptt)
    val testSet = buildVariableGapSequence(TEST_TRIALS, GAPS, Random(TEST_SEED))

    withGui {
        val accuracyPlot = addTimeSeriesComponent("Accuracy by gap", CONDITIONS)

        createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {

            val statusText = addLabelledText("Last run: ", "nothing trained yet")

            /**
             * Fresh weights each time, so the two conditions are compared from the same starting point
             * rather than one inheriting what the other learned.
             */
            suspend fun trainOn(condition: Int, gaps: List<Int>) {
                trainer.randomize()
                trainer.iteration = 0
                bptt.trainingSet = buildVariableGapSequence(TRAINING_TRIALS, gaps, Random(TRAINING_SEED))
                repeat(EPOCHS_PER_CLICK) { trainer.trainOnce() }

                val byGap = with(network) { bptt.measureAccuracyByGap(testSet) }
                accuracyPlot.model.timeSeriesList[condition].series.apply {
                    clear()
                    GAPS.forEach { add(it, byGap.getValue(it)) }
                }
                statusText.text = "${CONDITIONS[condition]}, mean ${byGap.values.average().asPercent()}"
            }

            addButton("Train on all gaps (${GAPS.first()}-${GAPS.last()})") { trainOn(0, GAPS) }
            addButton("Train on long gaps only (${LONG_GAPS.first()}-${LONG_GAPS.last()})") {
                trainOn(1, LONG_GAPS)
            }
            addButton("Clear chart") {
                accuracyPlot.model.timeSeriesList.forEach { it.series.clear() }
                statusText.text = "nothing trained yet"
            }
        }

        place(networkComponent, SIM_WINDOW_GAP, 250, 700, 560)
        place(accuracyPlot, SIM_WINDOW_GAP + 700 + SIM_WINDOW_GAP, 250, 560, 560)
    }

    addSidebarInfo(
        """
        # Curriculum

        A symbol is shown, then after a gap of anywhere from ${GAPS.first()} to ${GAPS.last()} steps a
        recall cue asks for it back. The network is unrolled over only $TRUNCATION_DEPTH steps, so for
        every gap longer than that the error at the recall has no gradient path back to the symbol.

        Train it on the full range of gaps and it answers all of them, including the longest. Train it on
        the long gaps alone and it answers none of them. Both training sets are the same size, and the
        second one contains _more_ examples of the long gaps than the first does.

        # Simulation Details

        Truncation depth is usually described as the longest dependency a network can learn. This
        simulation is a counterexample. What a $TRUNCATION_DEPTH-step window can learn from a
        ${GAPS.first()}-step gap is a rule for carrying a symbol across one step, and that rule composes:
        applying it repeatedly carries the symbol any distance at all. The gradient never spans seven steps;
        it does not have to, because the recurrent weights it shapes are applied seven times.

        So the depth limits which dependencies can be _discovered_, not which can be _represented_. Give
        the network a ladder of short dependencies and it climbs to the long ones on its own. Take the
        ladder away and a window this shallow has nothing to work from.

        Trials here are independent of one another, so the training data declares its sequence length and
        the network's memory is cleared at every trial boundary. Nothing a trial ends holding leaks into
        the next one.

        ## Control Panel Settings

        Each `Train` button randomizes the weights first, so the two conditions start from the same place
        rather than one inheriting the other's learning. Results stay on the chart until `Clear chart`, so
        the two curves can be compared directly.

        # What to Do

        1. Click `Train on all gaps`. The curve should sit at or near 100% across every gap length.

        2. Click `Train on long gaps only`. The curve collapses to chance
           (${(100.0 / ALPHABET_SIZE).format(0)}%) at every gap, including the ones it was trained on.

        3. Look at the `Output` layer after the second run. It answers with the same symbol every trial
           regardless of the cue, exactly as a network trained at too shallow a depth does in the
           [Delayed recall](Delayed recall) simulation. Failing to discover the memory rule and failing to
           have a deep enough window look identical from the outside, because they are the same failure.

        4. Raise `Truncation depth` in the trainer properties and retrain on long gaps only. It improves,
           but far less reliably than the ladder of short gaps did, which is the point: matching the depth
           to the dependency is the expensive way to solve this, and the data can often solve it for free.

        # References

        The task is a construction for this simulation rather than a reproduction of a published
        experiment. The effect it shows is the reason curriculum ordering is a common trick when training
        recurrent networks on long dependencies.
        """.trimIndent()
    )

}

/**
 * Grouped rather than left as top-level constants because names this generic would sit in the same
 * package as every other demo simulation.
 */
object Curriculum {
    const val ALPHABET_SIZE = 3

    /** Cue at row 0, recall somewhere in the rows after it, blanks everywhere else. */
    const val TRIAL_LENGTH = 8

    val GAPS = (1..TRIAL_LENGTH - 1).toList()
    val LONG_GAPS = GAPS.takeLast(3)

    /**
     * Larger than the delayed recall simulation uses, for the opposite reason: there the network had to be
     * kept from storing the symbol by accident, whereas here it has to succeed whenever it genuinely can.
     */
    const val HIDDEN_UNITS = 8

    /** Shallow on purpose, and shallower than every gap but the first. */
    const val TRUNCATION_DEPTH = 2

    const val LEARNING_RATE = 0.01
}

private const val TRAINING_TRIALS = 56
private const val TEST_TRIALS = 70
private const val EPOCHS_PER_CLICK = 400
private const val TRAINING_SEED = 1
private const val TEST_SEED = 2

private val SYMBOL_NAMES = listOf("A", "B", "C")
private val CONDITIONS = listOf("trained on all gaps", "trained on long gaps only")

/**
 * Fixed length trials whose recall step falls at one of [gaps], spread evenly over them so that changing
 * the range changes what the data teaches without changing how much of it there is.
 *
 * Declares its sequence length, since the trials really are independent: the trainer clears memory at each
 * boundary and never unrolls a window across one.
 */
fun buildVariableGapSequence(trials: Int, gaps: List<Int>, random: Random): TrainingDataset {
    val order = List(trials) { gaps[it % gaps.size] }.shuffled(random)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    order.forEach { gap ->
        val symbol = random.nextInt(ALPHABET_SIZE)
        repeat(TRIAL_LENGTH) { row ->
            inputs.add(
                when (row) {
                    0 -> oneHot(ALPHABET_SIZE + 1, symbol)
                    gap -> oneHot(ALPHABET_SIZE + 1, ALPHABET_SIZE)
                    else -> MutableList(ALPHABET_SIZE + 1) { 0.0 }
                }
            )
            targets.add(if (row == gap) oneHot(ALPHABET_SIZE, symbol) else MutableList(ALPHABET_SIZE) { 0.0 })
        }
    }
    return TrainingDataset(
        inputs, targets, ALPHABET_SIZE + 1, ALPHABET_SIZE, sequenceLength = TRIAL_LENGTH
    )
}

/**
 * Accuracy at the recall step of each trial, reported separately for each gap length, since the whole
 * question is which spans were learned rather than how many trials went right.
 */
context(Network)
fun BPTTNetwork.measureAccuracyByGap(testSet: TrainingDataset): Map<Int, Double> {
    val correct = mutableMapOf<Int, Int>()
    val total = mutableMapOf<Int, Int>()
    testSet.inputs.forEachIndexed { row, input ->
        // Cleared at each trial, matching the resets the declared sequence length causes during training.
        if (row % TRIAL_LENGTH == 0) resetRecurrentState()
        inputLayer.setActivations(input.toDoubleArray())
        forwardPass()
        val target = testSet.targets[row]
        if (target.any { it > 0.0 }) {
            val gap = row % TRIAL_LENGTH
            total[gap] = (total[gap] ?: 0) + 1
            if (outputLayer.activationArray.indexOfMax() == target.indexOfMax()) {
                correct[gap] = (correct[gap] ?: 0) + 1
            }
        }
    }
    return GAPS.associateWith { (correct[it] ?: 0).toDouble() / (total[it] ?: 1) }
}

private fun oneHot(size: Int, index: Int) = MutableList(size) { if (it == index) 1.0 else 0.0 }

private fun DoubleArray.indexOfMax() = withIndex().maxBy { it.value }.index

private fun List<Double>.indexOfMax() = withIndex().maxBy { it.value }.index

private fun Double.asPercent() = "${(this * 100).format(1)}%"
