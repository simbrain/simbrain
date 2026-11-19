/**
 * Learned gating, the second step toward PBWM. The task and network are those of the working memory (1 stripe)
 * simulation, but the hand-wired gate is replaced by basal ganglia Go and NoGo units that learn from dopamine. A
 * critic compares reward at recall to the reward it expects, and the difference, a reward prediction error, is the
 * dopamine signal. Gate decisions are tagged when made, so dopamine arriving at recall can change the weights that
 * produced them. The model logic lives in [LearnedGatingModel] so it can be tested headlessly.
 */
package org.simbrain.custom_sims.simulations.leabra

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.leabraLayer
import org.simbrain.network.trainers.leabraProjection
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import kotlin.math.abs
import kotlin.random.Random

val learnedGating = newSim("leabra_learned_gating") {
    workspace.clearWorkspace()
    exposeTypes(LeabraRule::class)

    val networkComponent = addNetworkComponent("Leabra network")
    val network = networkComponent.network

    fun labeledLayer(size: Int, label: String, labels: List<String>, x: Int, y: Int) = leabraLayer(size, label).apply {
        circleMode = true
        circleSpacingX = 60.0
        labelArray = labels.toTypedArray()
        location = point(x, y)
    }

    val task = labeledLayer(WmStep.entries.size, WM_TASK, WmStep.entries.map { it.label }, -330, 300).apply {
        circleSpacingX = 85.0
    }
    val input = labeledLayer(ITEMS.size, WM_INPUT, ITEMS, 130, 300)
    val memory = labeledLayer(ITEMS.size, PFC, ITEMS, 460, 0).apply { circleSpacingX = 85.0 }
    val hidden = leabraLayer(WM_HIDDEN_SIZE, WM_HIDDEN).apply {
        gridMode = true
        gridColumns = 5
        location = point(0, 0)
    }
    val output = labeledLayer(ITEMS.size, WM_OUTPUT, ITEMS, 0, -340).apply { circleSpacingX = 70.0 }
    val basalGanglia = NeuronArray(2).apply {
        label = BG_LABEL
        circleMode = true
        circleSpacingX = 85.0
        labelArray = arrayOf("Go", "NoGo")
        location = point(-100, 850)
        activations.fill(0.0)
    }
    val dopamine = NeuronArray(1).apply {
        label = DOPAMINE_LABEL
        circleMode = true
        location = point(550, 850)
        activations.fill(0.0)
    }
    val projections = listOf(
        leabraProjection(task, hidden),
        leabraProjection(input, hidden),
        leabraProjection(memory, hidden),
        leabraProjection(hidden, output),
        leabraProjection(output, hidden, FEEDBACK_SCALE),
    )
    projections.forEach { it.weights.label = WM_WEIGHTS_LABEL }
    val gatingWeights = listOf(leabraProjection(task, basalGanglia), leabraProjection(input, basalGanglia))
    gatingWeights.forEach { it.weights.label = GATING_WEIGHTS_LABEL }
    // Drawn only to show where dopamine goes: excitatory on Go (D1 receptors), inhibitory on NoGo (D2 receptors).
    val dopamineProjection = WeightMatrix(dopamine, basalGanglia).apply {
        label = DOPAMINE_PROJECTION_LABEL
        weights[GO, 0] = 1.0
        weights[NOGO, 0] = -1.0
    }
    showDopamineAndGatingRanges(dopamine, basalGanglia)
    val gateText = leabraMessage(gateMessage(null, false, null), GATE_TEXT, 460, 120)
    val dopamineText = leabraMessage(dopamineMessage(null), DOPAMINE_TEXT, 100, 990)
    val resultText = resultLabel(RESULT_TEXT, 250, -370)
    network.addNetworkModels(
        listOf(task, input, memory, hidden, output, basalGanglia, dopamine, gateText, dopamineText, resultText) +
            (projections + gatingWeights).map { it.weights } + dopamineProjection,
        usePlacementManager = false
    )

    addTimeSeriesComponent(ERROR_PLOT, listOf(RECALL_SERIES, GATE_SERIES))

    setUpLearnedGating(workspace)

    addSidebarInfo(SIDEBAR.trimIndent(), width = 330)
}.registerReopenFunction { workspace -> setUpLearnedGating(workspace) }

/**
 * The working memory network with a learned gate. On each step with an item, Go and NoGo in [basalGanglia] are the
 * mean of their gating weights from the active task and input units, and the gate opens if Go, plus exploration
 * [noise] while training, beats NoGo. Each gate decision tags the task and input units active at the time, with the
 * sign of the choice. At recall, [reward] turns the outcome into dopamine, which strengthens the choices that were
 * made (Go for an open gate, NoGo for a closed one) when positive, and weakens them when negative.
 *
 * Three choices make the gate and the output learn together, which they must since neither can improve while the
 * other is random:
 * - Reward is a forced choice, the most active output unit, so a weak but right answer still counts. Requiring a
 *   clearly active output gives almost no reward early on.
 * - Closed gates are tagged as well as open ones. Otherwise early failures teach the gate to stay closed, after
 *   which nothing is tagged and nothing learns.
 * - The rest of the network learns more slowly than in the one-stripe simulation. It learns to report working
 *   memory only from the sequences where memory is right, and a faster rate lets the others undo that.
 */
class LearnedGatingModel(network: Network, var random: Random = Random.Default) : WorkingMemoryModel(network, PFC) {

    val basalGanglia = layer(BG_LABEL)
    val dopamine = layer(DOPAMINE_LABEL)

    val gatingWeights = network.getModels<WeightMatrix>()
        .filter { it.target == basalGanglia && (it.source == task || it.source == input) }

    /**
     * Standard deviation of the noise added to Go minus NoGo when deciding whether to open the gate. Noise is
     * added only while training, so that the gate explores during practice and shows its learned choice otherwise.
     */
    var noise = 0.1

    private var exploring = false

    private var minusOutput = DoubleArray(ITEMS.size)

    init {
        trainer.learningRate = OUTPUT_LEARNING_RATE
    }

    override fun test(trial: WmTrial, fireEvents: Boolean): Int? {
        val response = super.test(trial, fireEvents)
        minusOutput = output.activationArray
        return response
    }

    var gatingLearningRate = 0.3

    /**
     * How fast the expected reward moves toward each reward received.
     */
    var criticLearningRate = 0.1

    /**
     * The reward the critic expects at recall.
     */
    var expectedReward by dopamine::expectedReward

    private val tags = gatingWeights.associateWith { DoubleArray(it.source.size) }

    /**
     * Gate decisions made, and how many opened on a distractor or stayed closed on a remember step, since
     * [resetGateCounts].
     */
    var gateDecisions = 0
        private set
    var gateMistakes = 0
        private set

    fun resetGateCounts() {
        gateDecisions = 0
        gateMistakes = 0
    }

    override fun decideGate(trial: WmTrial): Boolean {
        setDopamine(0.0)
        setBasalGanglia(DoubleArray(2))
        if (trial.item == null) return false
        val acts = DoubleArray(2) { j ->
            gatingWeights.sumOf { m -> m.weights.mv(m.source.activations.col(0))[j] } / gatingWeights.size
        }
        setBasalGanglia(acts)
        val open = acts[GO] - acts[NOGO] + (if (exploring) noise * random.gaussian() else 0.0) > 0
        val sign = if (open) 1.0 else -1.0
        for ((m, tag) in tags) {
            val x = (m.source as NeuronArray).activationArray
            for (i in tag.indices) tag[i] += sign * x[i]
        }
        gateDecisions++
        if (open != (trial.step == WmStep.REMEMBER)) gateMistakes++
        return open
    }

    /**
     * Deliver reward at recall, 1 if the most active unit in [outputActs] is the item to recall and 0 otherwise,
     * and return dopamine, the reward minus the reward expected. If [learn], dopamine changes the gating weights of tagged units and the
     * expected reward moves toward the reward. Tags are cleared either way.
     */
    fun reward(learn: Boolean, outputActs: DoubleArray = output.activationArray): Double {
        val target = toRecall
        val r = if (target != null && outputActs.indices.maxBy { outputActs[it] } == target) 1.0 else 0.0
        val da = r - expectedReward
        setDopamine(da)
        if (learn) {
            for ((m, tag) in tags) {
                learnGating(m.weights, GO, NOGO, tag, da, gatingLearningRate)
                m.events.updated.fire()
            }
            expectedReward += criticLearningRate * da
        }
        tags.values.forEach { it.fill(0.0) }
        return da
    }

    override fun train(trial: WmTrial): Boolean {
        exploring = true
        val correct = try {
            super.train(trial)
        } finally {
            exploring = false
        }
        if (trial.step == WmStep.RECALL) reward(learn = true, outputActs = minusOutput)
        return correct
    }

    override fun clear() {
        super.clear()
        setBasalGanglia(DoubleArray(2))
        setDopamine(0.0)
        tags.values.forEach { it.fill(0.0) }
    }

    override fun reset() {
        super.reset()
        gatingWeights.forEach {
            it.randomize(UniformRealDistribution(0.25, 0.75))
            it.events.updated.fire()
        }
        expectedReward = 0.0
    }

    private fun setBasalGanglia(acts: DoubleArray) = basalGanglia.showActivations(acts)

    private fun setDopamine(value: Double) = dopamine.showActivations(doubleArrayOf(value))
}

private fun gateMessage(step: WmStep?, open: Boolean, item: Int?): String {
    val prefix = when {
        step == null || step == WmStep.RECALL -> "Gate closed"
        open -> "Go wins: gate open"
        else -> "NoGo wins: gate closed"
    }
    return when {
        item == null -> "$prefix, memory empty"
        open -> "$prefix, storing ${ITEMS[item]}"
        else -> "$prefix, keeping ${ITEMS[item]}"
    }
}

private fun rewardMessage(reward: Double, guess: Int, expected: Double) =
    "Reward %.0f (output says %s), expected %.2f".format(reward, ITEMS[guess], expected)

private fun dopamineMessage(da: Double?): String = when {
    da == null -> "Dopamine: waiting for recall"
    abs(da) < DOPAMINE_THRESHOLD -> "Dopamine %+.2f: as expected".format(da)
    da > 0 -> "Dopamine %+.2f: better than expected".format(da)
    else -> "Dopamine %+.2f: worse than expected".format(da)
}

private const val PFC = "Working memory (PFC)"
private const val OUTPUT_LEARNING_RATE = 0.03
private const val GATE_TEXT = "Gate"
private const val DOPAMINE_TEXT = "Dopamine message"
private const val RESULT_TEXT = "Result"
private const val ERROR_PLOT = "Mistakes"
private const val RECALL_SERIES = "Wrong recalls (%)"
private const val GATE_SERIES = "Wrong gate decisions (%)"
private const val PRACTICE_SEQUENCES = 100
private const val PLOT_BLOCK = 10

private suspend fun SimulationScope.setUpLearnedGating(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val model = LearnedGatingModel(network)
    val texts = network.getModels<NetworkTextObject>()
    val gateText = texts.first { it.label == GATE_TEXT }
    val dopamineText = texts.first { it.label == DOPAMINE_TEXT }
    val resultText = texts.first { it.label == RESULT_TEXT }
    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == ERROR_PLOT }
    var sequences = PLOT_BLOCK * (plot.model.timeSeriesList.firstOrNull()?.series?.itemCount ?: 0)

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Leabra cycle") { model.trainer.cycle() })

    fun showGate(step: WmStep? = null) {
        gateText.text = gateMessage(step, model.gateOpen, model.remembered)
    }

    suspend fun runStep(step: WmStep, item: Int?) {
        workspace.stop()
        model.startTrial(WmTrial(step, item))
        showGate(step)
        dopamineText.text = dopamineMessage(null)
        resultText.showResult(null)
        workspace.iterateSuspend(model.trainer.minusCycles)
        if (step == WmStep.RECALL) {
            val acts = model.output.activationArray
            val guess = acts.indices.maxBy { acts[it] }
            resultText.showResult(guess == model.toRecall)
            val expected = model.expectedReward
            val da = model.reward(learn = false)
            dopamineText.text = rewardMessage(da + expected, guess, expected) + "\n" + dopamineMessage(da)
        }
    }

    fun practice() {
        workspace.stop()
        repeat(PRACTICE_SEQUENCES / PLOT_BLOCK) {
            model.resetGateCounts()
            val wrong = model.practice(PLOT_BLOCK)
            sequences += PLOT_BLOCK
            plot.model.addData(0, sequences.toDouble(), 100 * wrong)
            plot.model.addData(1, sequences.toDouble(), 100.0 * model.gateMistakes / model.gateDecisions)
        }
        model.clear()
        showGate()
        dopamineText.text = dopamineMessage(null)
        resultText.showResult(null)
    }

    withGui {
        val controlPanel = createControlPanel("Learned gating", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            for (step in listOf(WmStep.REMEMBER, WmStep.DISTRACTOR)) {
                addRow(step.label) {
                    ITEMS.forEachIndexed { index, item ->
                        addButton(item) { runStep(step, index) }.apply {
                            toolTipText = if (step == WmStep.REMEMBER) {
                                "Show $item as an item to remember. The basal ganglia decide whether to store it."
                            } else {
                                "Show $item as a distractor. The basal ganglia decide whether to store it."
                            }
                        }
                    }
                }
            }
            addButton(WmStep.RECALL.label) { runStep(WmStep.RECALL, null) }.apply {
                toolTipText = "See what the network recalls and the dopamine it produces."
            }
            addSeparator()
            addButton("Practice $PRACTICE_SEQUENCES sequences") { practice() }.apply {
                toolTipText = "Practice on $PRACTICE_SEQUENCES random sequences, learning as it goes, and plot the mistakes."
            }
            addSeparator()
            addButton("Reset") {
                workspace.stop()
                model.reset()
                showGate()
                dopamineText.text = dopamineMessage(null)
                resultText.showResult(null)
                sequences = 0
                plot.model.clearData()
            }.apply { toolTipText = "Randomize the weights, empty memory, reset expected reward, and clear the plot." }
        }.awaitLayout()
        val networkX = controlPanel.rightEdgeWithGap()
        place(networkComponent, networkX, SIM_WINDOW_GAP, 640, 640)
        place(plot, networkX + 640 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
    }
}

private const val SIDEBAR = """
# Learned gating

In the `Working memory (1 stripe)` simulation, earlier in this Leabra series, you opened the working memory gate by
hand. Here the basal ganglia learn when to open it, by trial and error, guided by dopamine. This is the core idea of
PBWM (prefrontal cortex basal ganglia working memory). It illustrates three principles:

1. The basal ganglia (BG) decide when working memory takes in a new item. `Go` units vote to open the gate and
   `NoGo` units vote to keep it closed. During practice, a little randomness lets the BG try both.
2. Dopamine signals whether things went better or worse than expected, not reward itself. When a reward is fully
   expected, dopamine stays near zero.
3. Dopamine arrives at recall, after the gate decisions that earned it. Each time the BG decide, they tag the inputs
   active at the time, and dopamine at recall changes the weights of the tagged inputs.

This simulation builds on the `Working memory (1 stripe)` simulation, and uses the same task, network, and learning.
What it adds is a basal ganglia gate and a dopamine signal that trains it.

# Simulation Details

The task is the one in the `Working memory (1 stripe)` simulation: remember an item, see zero to two distractors,
then recall. Each button press is one step. On `Recall`, a label next to `Output` says ✓ CORRECT in green or ✗ WRONG
in red, based on its most active unit.

On each step with an item, `Go` and `NoGo` in `Basal ganglia (BG)` are driven by `Task` and `Input` through the
`Gating weights`. If `Go` beats `NoGo` the gate opens and `Working memory (PFC)` stores the
item. The message next to working memory says which won.

At recall, there is a reward if the most active `Output` unit is the item to recall: the network's best guess
counts, even if it is weak. `Dopamine (SNc)` compares the reward to the reward it expects, a running average of past
rewards, and the message below it shows the difference. In PBWM the expected reward comes from a separate learning
system (PVLV); here it is a simple average.

Dopamine projects to the BG, as the `Dopamine` arrow shows. Positive dopamine strengthens the choices the BG made
on the tagged inputs: `Go` where the gate opened, `NoGo` where it stayed closed. Negative dopamine weakens them.
`Go` and `NoGo` units respond to dopamine in opposite ways, through D1 and D2 dopamine receptors.

The gate and the rest of the network have to learn together. The output cannot learn to report working memory
while the gate stores the wrong items, and the gate gets no useful reward while the output cannot report memory.
Each small improvement in one helps the other, so learning starts slowly and then speeds up.

The rest of the network learns as in the `Working memory (1 stripe)` simulation, but more slowly. The plot shows two
kinds of mistakes in each block of 10 sequences: wrong recalls, and wrong gate decisions (opening on a distractor or
staying closed on a remember step).

## Control Panel Settings

- `Remember` `A`, `B`, `C`: Show the item as one to remember. The BG decide whether to store it.
- `Distractor` `A`, `B`, `C`: Show the item as a distractor. The BG decide whether to store it.
- `Recall`: See what the network recalls, the reward, and the dopamine this produces.
- `Practice $PRACTICE_SEQUENCES sequences`: Practice on $PRACTICE_SEQUENCES random sequences, learning as it goes,
  and plot the mistakes.
- `Reset`: Randomize the weights, empty memory, reset the expected reward, and clear the plot.

The `Remember`, `Distractor`, and `Recall` buttons run the network without learning, and there is no need to press
play. Only `Practice $PRACTICE_SEQUENCES sequences` learns.

# Activities

## Activity 1: An untrained gate
1. Click `Remember` `A`, `Distractor` `B`, and `Distractor` `C`.
    - Watch `Go` and `NoGo` and the gate message on each step. The BG have not learned anything, so whether the
      gate opens depends on random starting weights, not on whether the item should be remembered.
2. Click `Recall`.
    - `Output` rarely shows `A`. The message shows the reward: 1 if the most active `Output` unit is `A`, 0
      otherwise. A new network expects no reward, so dopamine equals the reward: `+1.00`, better than expected, or
      `0.00`.

## Activity 2: Learn when to open the gate
1. Click `Practice $PRACTICE_SEQUENCES sequences` until both lines fall to near 0.
    - This usually takes several clicks. Mistakes stay high at first, then both lines fall together, as the gate and
      the rest of the network help each other learn.
    - Some networks get stuck. If the lines are still high after 10 clicks, click `Reset` and start
      again.

## Activity 3: Test the trained gate
1. Repeat Activity 1.
    - The gate opens on `Remember` `A` and stays closed on the distractors. `Output` stays low until `Recall`, and
      then shows `A`.
    - The reward is 1 and the expected reward is close to 1, so dopamine is near zero (principle 2).
2. Look at the `Gating weights` from `Task` to `Basal ganglia (BG)`.
    - `Remember` drives `Go` more than `NoGo`, and `Distractor` the reverse.

## Explore further
- Reset and practice again, watching the dopamine message during the first few recalls of a new network.
- Compare the plot to the one in the `Working memory (1 stripe)` simulation, where the gate was given. Why is
  learning slower here?

# References

O'Reilly, R. C., & Frank, M. J. (2006). Making working memory work: A computational model of learning in the
prefrontal cortex and basal ganglia. _Neural Computation, 18_(2), 283–328.

O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
[_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
"""
