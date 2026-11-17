/**
 * Working memory with one hand-gated stripe, the first step toward PBWM. An item is remembered, distractors go by,
 * and on recall the network should report the remembered item. The working memory layer holds one item between
 * steps, and a hand-wired gate decides when it takes in a new one. The rest of the network learns with
 * [LeabraTrainer]. The task logic lives in [WorkingMemoryModel] so it can be tested headlessly.
 */
package org.simbrain.custom_sims.simulations.leabra

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.trainers.LeabraTrainer
import org.simbrain.network.trainers.leabraLayer
import org.simbrain.network.trainers.leabraProjection
import org.simbrain.network.trainers.leabraProjections
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import kotlin.random.Random

val workingMemoryOneStripe = newSim("leabra_working_memory_1_stripe") {
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

    val task = labeledLayer(WmStep.entries.size, TASK, WmStep.entries.map { it.label }, -440, 0).apply { circleSpacingX = 85.0 }
    val input = labeledLayer(ITEMS.size, INPUT, ITEMS, 0, 300)
    val memory = labeledLayer(ITEMS.size, MEMORY, ITEMS, 460, 0).apply { circleSpacingX = 85.0 }
    val hidden = leabraLayer(HIDDEN_SIZE, HIDDEN).apply {
        gridMode = true
        gridColumns = 5
        location = point(0, 0)
    }
    val output = leabraLayer(ITEMS.size, OUTPUT).apply {
        circleMode = true
        circleSpacingX = 70.0
        labelArray = ITEMS.toTypedArray()
        location = point(0, -340)
    }
    val projections = listOf(
        leabraProjection(task, hidden),
        leabraProjection(input, hidden),
        leabraProjection(memory, hidden),
        leabraProjection(hidden, output),
        leabraProjection(output, hidden, FEEDBACK_SCALE),
    )
    projections.forEach { it.weights.label = WEIGHTS_LABEL }
    val gateText = NetworkTextObject(gateMessage(false, false, null)).apply {
        fontSize = 16
        isBold = true
        color = LEABRA_MESSAGE_COLOR
        location = point(460, 80)
    }
    network.addNetworkModels(
        listOf(task, input, memory, hidden, output, gateText) + projections.map { it.weights },
        usePlacementManager = false
    )

    addTimeSeriesComponent(ERROR_PLOT, ERROR_SERIES)

    setUpWorkingMemory(workspace)

    addSidebarInfo(
        """
        # Working memory (1 stripe)

        Working memory holds something in mind while other things go by, like remembering what a friend asked you to
        buy while you walk past ads for other things. This simulation shows the simplest version: a working memory
        with one stripe, which holds one item, and a gate that decides when a new item gets in. It is the first step
        toward PBWM (prefrontal cortex basal ganglia working memory), a Leabra model of working memory. It
        illustrates three principles:

        1. Working memory needs a gate. A memory that takes in everything loses what it was holding to the next
           distraction.
        2. Holding the right item is not enough. The rest of the network has to learn when and how to use what
           memory holds, using the learning shown in the `Error-driven learning` simulation.
        3. Something has to decide when to open the gate. Here you turn it on by hand. In PBWM the basal ganglia
           learn when to open it, which is the subject of a later simulation.

        This simulation builds on the earlier Leabra simulations. Units are point neurons (`Point neuron`), each
        layer has the same inhibition (`Layer inhibition`), and the network learns as in `Error-driven learning`.
        What it adds is a working memory and a gate.

        # Simulation Details

        Each button press is one step. `Task` shows whether the step is `Remember`, `Distractor`, or `Recall`, and
        `Input` shows the item, which is empty on `Recall`. `Output` should stay low until `Recall`, and then show
        the item last remembered. This is a version of the Store-Ignore-Recall task used to test PBWM.

        `Memory (1 stripe)` is the working memory, and it holds one item. Without a gate it copies every item shown.
        With the gate, it copies only remembered items and holds each one, unchanged, until the next. PBWM's stripes
        work in a similar way: when gated, a stripe copies its input into units that maintain it. The copy and the
        gate are wired by hand. The `Weights` are learned: through them, `Hidden` learns to report memory on
        `Recall` and to stay quiet otherwise.

        Learning works as in `Error-driven learning`. On each step the network first responds on its own, then is
        shown the correct response, and its weights change toward it. Practice uses random sequences: remember an item,
        see zero to two distractors (other items), then recall. The plot shows the percent of recalls that were wrong
        in each block of 10 sequences.

        ## Control Panel Settings

        - `Remember` `A`, `B`, `C`: Show the item and store it in working memory.
        - `Distractor` `A`, `B`, `C`: Show the item. With the gate on, it is not stored.
        - `Recall`: See what the network recalls.
        - `Use gate`: Let only remembered items into working memory, keeping distractors out.
        - `Practice $PRACTICE_SEQUENCES sequences`: Practice on $PRACTICE_SEQUENCES random sequences, learning as it
          goes, and plot the mistakes.
        - `Reset`: Randomize the weights, empty memory, and clear the plot.

        The `Remember`, `Distractor`, and `Recall` buttons run the network without learning, and there is no need to
        press play. Only `Practice $PRACTICE_SEQUENCES sequences` learns.

        # Activities

        The activities build working memory in stages: no gate and no learning, then a gate, then learning. The
        last activity removes the gate again.

        ## Activity 1: No gate, no learning
        1. Click `Remember` `A`, `Distractor` `B`, and `Distractor` `C`.
            - Memory copies each item in turn, and `No gate: storing C` appears. The distractors have overwritten `A`
              (principle 1).
        2. Click `Recall`.
            - `Output` rarely shows `A`. It may show another item, or nothing.

        ## Activity 2: Add a gate
        1. Check `Use gate`.
        2. Click `Remember` `A`, `Distractor` `B`, and `Distractor` `C`.
            - `Gate open: storing A` appears, then `Gate closed: keeping A`. Memory now holds the right item.
        3. Click `Recall`.
            - `Output` still rarely shows `A`. The network has not learned what to do with its memory (principle 2).

        ## Activity 3: Learn to use memory
        1. Click `Practice $PRACTICE_SEQUENCES sequences` two or three times.
            - Mistakes fall to near 0, with an occasional slip.
        2. Repeat Activity 2.
            - `Output` now stays low until `Recall`, and then shows `A`.

        ## Activity 4: Remove the gate after learning
        1. After Activity 3, uncheck `Use gate`.
        2. Repeat Activity 2's steps: `Remember` `A`, `Distractor` `B`, `Distractor` `C`, and `Recall`.
            - Memory ends up holding `C`, and the network recalls `C`. It still reports its memory faithfully, but
              memory now holds the wrong item.
        3. Click `Practice $PRACTICE_SEQUENCES sequences`.
            - Mistakes jump above 50% and stay there. Learning cannot make up for a missing gate.

        ## Explore further
        - After Activity 4, check `Use gate` and practice again. How quickly does the network recover?
        - With the gate on, remember `A`, then see `A` as a distractor. Would the gate have mattered?

        # References

        O'Reilly, R. C., & Frank, M. J. (2006). Making working memory work: A computational model of learning in the
        prefrontal cortex and basal ganglia. _Neural Computation, 18_(2), 283–328.

        O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
        [_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
        """.trimIndent(),
        width = 330
    )
}.registerReopenFunction { workspace -> setUpWorkingMemory(workspace) }

enum class WmStep(val label: String) {
    REMEMBER("Remember"), DISTRACTOR("Distractor"), RECALL("Recall");

    override fun toString() = label
}

/**
 * One step: its kind and, except on recall, the index of the item shown.
 */
data class WmTrial(val step: WmStep, val item: Int?)

val ITEMS = listOf("A", "B", "C")

const val MAX_DISTRACTORS = 2

/**
 * Remember a random item, see up to [MAX_DISTRACTORS] other items as distractors, then recall.
 */
fun wmSequence(random: Random = Random.Default): List<WmTrial> {
    val remembered = random.nextInt(ITEMS.size)
    val others = ITEMS.indices.filter { it != remembered }
    val distractors = List(random.nextInt(MAX_DISTRACTORS + 1)) { WmTrial(WmStep.DISTRACTOR, others.random(random)) }
    return listOf(WmTrial(WmStep.REMEMBER, remembered)) + distractors + WmTrial(WmStep.RECALL, null)
}

/**
 * The working memory network in [network] together with its gate and the item the task says to recall.
 */
class WorkingMemoryModel(val network: Network) {

    private val arrays = network.getModels<NeuronArray>()
    private fun layer(name: String) = arrays.first { it.label == name }

    val task = layer(TASK)
    val input = layer(INPUT)
    val memory = layer(MEMORY)
    val hidden = layer(HIDDEN)
    val output = layer(OUTPUT)

    val trainer = run {
        val layers = listOf(task, input, memory, hidden, output)
        val projections = leabraProjections(network, layers, FEEDBACK_SCALE)
        LeabraTrainer(network, layers, projections, input, output).apply {
            expectedActivity[task] = 1.0 / WmStep.entries.size
            expectedActivity[input] = 1.0 / ITEMS.size
            expectedActivity[memory] = 1.0 / ITEMS.size
            expectedActivity[output] = 1.0 / ITEMS.size
        }
    }

    /**
     * Whether memory is gated. Without a gate, every item shown gets in; with it, only remembered items do.
     */
    var useGate = false

    /**
     * Whether the gate opened on the most recent step.
     */
    var gateOpen = false
        private set

    /**
     * The item last shown on a remember step, which recall should report.
     */
    var toRecall: Int? = null
        private set

    /**
     * The item working memory holds, if any.
     */
    val remembered: Int? get() = memory.activationArray.indices.firstOrNull { memory.activationArray[it] > 0.5 }

    /**
     * The correct output: the item to recall on a recall step, and nothing otherwise.
     */
    fun target(trial: WmTrial) = if (trial.step == WmStep.RECALL) toRecall else null

    /**
     * Clamp the task, open or close the gate, and reset the rest of the network, ready for a minus phase.
     */
    fun startTrial(trial: WmTrial) {
        if (trial.step == WmStep.REMEMBER) toRecall = trial.item
        val item = oneHot(ITEMS.size, trial.item)
        trainer.clamp(task, oneHot(WmStep.entries.size, trial.step.ordinal))
        gateOpen = trial.item != null && (!useGate || trial.step == WmStep.REMEMBER)
        if (gateOpen) trainer.clamp(memory, item) else memory.isClamped = true
        trainer.startTrial(item)
    }

    /**
     * The item the network outputs, or null if no output unit is clearly active.
     */
    fun response(): Int? {
        val acts = output.activationArray
        val best = acts.indices.maxBy { acts[it] }
        return best.takeIf { acts[it] > 0.5 }
    }

    /**
     * Run a step without learning and return the network's output.
     */
    fun test(trial: WmTrial, fireEvents: Boolean = false): Int? {
        startTrial(trial)
        repeat(trainer.minusCycles) { trainer.cycle(fireEvents) }
        return response()
    }

    /**
     * Run a step with a minus and plus phase and learn. Returns whether the minus phase output was right.
     */
    fun train(trial: WmTrial): Boolean {
        val response = test(trial)
        val target = target(trial)
        val minus = trainer.snapshot()
        trainer.clamp(output, oneHot(ITEMS.size, target))
        repeat(trainer.plusCycles) { trainer.cycle(false) }
        trainer.learn(minus, trainer.snapshot())
        return response == target
    }

    /**
     * Practice [count] sequences and return the fraction of recalls that were wrong.
     */
    fun practice(count: Int, random: Random = Random.Default): Double {
        val wrong = (1..count).count { wmSequence(random).map { train(it) }.last().not() }
        return wrong.toDouble() / count
    }

    /**
     * Empty working memory and the rest of the network, leaving weights alone.
     */
    fun clear() {
        listOf(task, input, memory).forEach { trainer.clamp(it, DoubleArray(it.size)) }
        trainer.resetActivity()
        trainer.layers.forEach { it.events.updated.fire() }
        toRecall = null
        gateOpen = false
    }

    /**
     * Randomize weights and empty working memory.
     */
    fun reset() {
        trainer.randomizeWeights()
        clear()
    }
}

private fun gateMessage(useGate: Boolean, open: Boolean, item: Int?): String {
    val prefix = if (!useGate) "No gate" else if (open) "Gate open" else "Gate closed"
    return when {
        item == null -> "$prefix: memory empty"
        open -> "$prefix: storing ${ITEMS[item]}"
        else -> "$prefix: keeping ${ITEMS[item]}"
    }
}

private const val TASK = "Task"
private const val INPUT = "Input"
private const val MEMORY = "Memory (1 stripe)"
private const val HIDDEN = "Hidden"
private const val OUTPUT = "Output"
private const val HIDDEN_SIZE = 25
private const val WEIGHTS_LABEL = "Weights"
private const val ERROR_PLOT = "Recall mistakes"
private const val ERROR_SERIES = "Wrong recalls (%)"
private const val PRACTICE_SEQUENCES = 100
private const val PLOT_BLOCK = 10

private suspend fun SimulationScope.setUpWorkingMemory(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val model = WorkingMemoryModel(network)
    val gateText = network.getModels<NetworkTextObject>().first()
    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == ERROR_PLOT }
    var sequences = PLOT_BLOCK * (plot.model.timeSeriesList.firstOrNull()?.series?.itemCount ?: 0)

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Leabra cycle") { model.trainer.cycle() })

    fun showGate(open: Boolean = model.gateOpen) {
        gateText.text = gateMessage(model.useGate, open, model.remembered)
    }

    suspend fun runStep(step: WmStep, item: Int?) {
        workspace.stop()
        model.startTrial(WmTrial(step, item))
        showGate()
        workspace.iterateSuspend(model.trainer.minusCycles)
    }

    fun practice() {
        workspace.stop()
        repeat(PRACTICE_SEQUENCES / PLOT_BLOCK) {
            val wrong = model.practice(PLOT_BLOCK)
            sequences += PLOT_BLOCK
            plot.model.addData(0, sequences.toDouble(), 100 * wrong)
        }
        model.clear()
        showGate()
    }

    withGui {
        val controlPanel = createControlPanel("Working memory", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            for (step in listOf(WmStep.REMEMBER, WmStep.DISTRACTOR)) {
                addRow(step.label) {
                    ITEMS.forEachIndexed { index, item ->
                        addButton(item) { runStep(step, index) }.apply {
                            toolTipText = if (step == WmStep.REMEMBER) {
                                "Show $item and store it in working memory."
                            } else {
                                "Show $item as a distractor. With the gate on, it is not stored."
                            }
                        }
                    }
                }
            }
            addButton(WmStep.RECALL.label) { runStep(WmStep.RECALL, null) }.apply {
                toolTipText = "See what the network recalls."
            }
            addSeparator()
            addCheckBox(
                "Use gate", model.useGate,
                toolTip = "Let only remembered items into working memory, keeping distractors out."
            ) {
                model.useGate = it
                showGate(open = false)
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
                sequences = 0
                plot.model.clearData()
            }.apply { toolTipText = "Randomize the weights, empty memory, and clear the plot." }
        }.awaitLayout()
        val networkX = controlPanel.rightEdgeWithGap()
        place(networkComponent, networkX, SIM_WINDOW_GAP, 640, 640)
        place(plot, networkX + 640 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
    }
}
