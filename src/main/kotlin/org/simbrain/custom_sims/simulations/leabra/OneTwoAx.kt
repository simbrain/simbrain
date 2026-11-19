/**
 * The 1-2-AX task, the capstone of the PBWM series. Two working memory stripes, each with its own Go/NoGo pair in
 * the basal ganglia, learn from one shared dopamine signal which inputs to hold: the context digit in one stripe and
 * the first letter of each pair in the other. Reward comes from whether working memory holds what the response
 * needs, not from the response itself; see [OneTwoAxModel] for why. The model logic lives in [OneTwoAxModel] so it
 * can be tested headlessly.
 */
package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.trainers.LeabraTrainer
import org.simbrain.network.trainers.leabraLayer
import org.simbrain.network.trainers.leabraProjection
import org.simbrain.network.trainers.leabraProjections
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.random.Random

val oneTwoAx = newSim("leabra_one_two_ax") {
    workspace.clearWorkspace()
    exposeTypes(LeabraRule::class)

    val networkComponent = addNetworkComponent("Leabra network")
    val network = networkComponent.network

    fun labeledLayer(size: Int, label: String, labels: List<String>, x: Int, y: Int) = leabraLayer(size, label).apply {
        circleMode = true
        circleSpacingX = 50.0
        labelArray = labels.toTypedArray()
        location = point(x, y)
    }

    val input = labeledLayer(AX_INPUTS.size, AX_INPUT, AX_INPUTS, 0, 300)
    val stripes = List(STRIPES) { labeledLayer(AX_INPUTS.size, stripeLabel(it), AX_INPUTS, -620 + 1240 * it, 0) }
    val hidden = leabraLayer(AX_HIDDEN_SIZE, AX_HIDDEN).apply {
        gridMode = true
        gridColumns = 8
        location = point(0, 0)
    }
    val output = labeledLayer(AX_OUTPUTS.size, AX_OUTPUT, AX_OUTPUTS, 0, -650).apply { circleSpacingX = 70.0 }
    val basalGanglia = NeuronArray(2 * STRIPES).apply {
        label = BG_LABEL
        circleMode = true
        circleSpacingX = 70.0
        labelArray = Array(2 * STRIPES) { (if (it % 2 == GO) "Go " else "NoGo ") + (it / 2 + 1) }
        location = point(0, 850)
        activations.fill(0.0)
    }
    val dopamine = NeuronArray(1).apply {
        label = DOPAMINE_LABEL
        circleMode = true
        location = point(900, 850)
        activations.fill(0.0)
    }
    val projections = listOf(leabraProjection(input, hidden)) + stripes.map { leabraProjection(it, hidden) } +
        listOf(leabraProjection(hidden, output), leabraProjection(output, hidden, FEEDBACK_SCALE))
    projections.forEach { it.weights.label = AX_WEIGHTS_LABEL }
    val gatingWeights = WeightMatrix(input, basalGanglia).apply {
        label = GATING_WEIGHTS_LABEL
        randomize(initialGatingWeights())
    }
    // Drawn only to show where dopamine goes: excitatory on Go (D1 receptors), inhibitory on NoGo (D2 receptors).
    val dopamineProjection = WeightMatrix(dopamine, basalGanglia).apply {
        label = DOPAMINE_PROJECTION_LABEL
        for (j in 0 until 2 * STRIPES) weights[j, 0] = if (j % 2 == GO) 1.0 else -1.0
    }
    showDopamineAndGatingRanges(dopamine, basalGanglia)
    network.addNetworkModels(
        listOf(input) + stripes + listOf(hidden, output, basalGanglia, dopamine) +
            projections.map { it.weights } + gatingWeights + dopamineProjection,
        usePlacementManager = false
    )

    addTimeSeriesComponent(AX_PLOT, listOf(AX_RESPONSE_SERIES, AX_MEMORY_SERIES))

    setUpOneTwoAx(workspace)

    addSidebarInfo(AX_SIDEBAR.trimIndent(), width = 330)
}.registerReopenFunction { workspace -> setUpOneTwoAx(workspace) }

val AX_INPUTS = listOf("1", "2", "A", "B", "C", "X", "Y", "Z")
val AX_OUTPUTS = listOf("L", "R")
private val DIGITS = 0..1
private val FIRST_LETTERS = 2..4
private val SECOND_LETTERS = 5..7
const val AX_L = 0
const val AX_R = 1

fun axIndex(label: String) = AX_INPUTS.indexOf(label)

/**
 * One step of the task: the input shown, the correct response, whether the response is rewarded (on the second
 * letter of a pair), and the inputs working memory must hold for the response to be possible.
 */
data class AxTrial(val input: Int, val target: Int, val rewarded: Boolean, val needed: List<Int>)

/**
 * The outcome of a trained step: the trial, the network's response, and the dopamine delivered, if any.
 */
data class AxStepResult(val trial: AxTrial, val response: Int, val dopamine: Double?) {
    val right get() = response == trial.target
}

/**
 * The trial for the last input in [history], the inputs shown so far. The context is the most recent digit. A
 * second letter is a target, answered R, if it completes A-X after a 1 or B-Y after a 2; everything else is
 * answered L.
 */
fun axTrial(history: List<Int>): AxTrial {
    val current = history.last()
    val digit = history.lastOrNull { it in DIGITS }
    val first = history.getOrNull(history.size - 2)?.takeIf { it in FIRST_LETTERS }
    val rewarded = current in SECOND_LETTERS
    val target = rewarded && (
        (digit == axIndex("1") && first == axIndex("A") && current == axIndex("X")) ||
            (digit == axIndex("2") && first == axIndex("B") && current == axIndex("Y"))
        )
    return AxTrial(current, if (target) AX_R else AX_L, rewarded, if (rewarded) listOfNotNull(digit, first) else emptyList())
}

const val AX_MAX_PAIRS = 4

/**
 * A random sequence of inputs: a digit, then one to [AX_MAX_PAIRS] letter pairs. A quarter of pairs are the target
 * for the context, a quarter are the target for the other context, and the rest are random non-targets, so that
 * neither the digit nor the pair alone gives the answer.
 */
fun axSequence(random: Random = Random.Default): List<Int> {
    val digit = random.nextInt(2)
    val targetPair = if (digit == 0) "A" to "X" else "B" to "Y"
    val lurePair = if (digit == 0) "B" to "Y" else "A" to "X"
    val inputs = mutableListOf(digit)
    repeat(1 + random.nextInt(AX_MAX_PAIRS)) {
        val p = random.nextDouble()
        val (first, second) = when {
            p < 0.25 -> targetPair
            p < 0.5 -> lurePair
            else -> generateSequence { listOf("A", "B", "C").random(random) to listOf("X", "Y", "Z").random(random) }
                .first { it != targetPair }
        }
        inputs += axIndex(first)
        inputs += axIndex(second)
    }
    return inputs
}

/**
 * The 1-2-AX network in [network]. On each step, each stripe's Go and NoGo are its gating weights from the input
 * shown, and the stripe takes in the input if Go, plus exploration [noise] while training, beats NoGo. Each decision
 * tags the input with its sign for that stripe. On the second letter of a pair, [reward] gives dopamine, the reward
 * minus the reward expected, which strengthens the tagged choices when positive and weakens them when negative.
 *
 * Reward is 1 if working memory holds what the response needs: the context digit and the first letter of the pair.
 * In PBWM, reward comes from whether the response is right, and [LearnedGatingModel] does that. Here it fails: the
 * response depends on three inputs together, so the rest of the network cannot learn to use working memory until
 * the gates are nearly right, and the gates get no useful dopamine until the rest of the network uses working
 * memory. Networks trained that way stay at always answering L. PBWM gets past this with a learned critic (PVLV),
 * output gating, more stripes, and much longer training.
 */
class OneTwoAxModel(val network: Network, var random: Random = Random.Default) {

    private val arrays = network.getModels<NeuronArray>()
    private fun layer(name: String) = arrays.first { it.label == name }

    val input = layer(AX_INPUT)
    val stripes = List(STRIPES) { layer(stripeLabel(it)) }
    val hidden = layer(AX_HIDDEN)
    val output = layer(AX_OUTPUT)
    val basalGanglia = layer(BG_LABEL)
    val dopamine = layer(DOPAMINE_LABEL)
    val gatingWeights = network.getModels<WeightMatrix>().first { it.target == basalGanglia && it.source == input }

    val trainer = run {
        val layers = listOf(input) + stripes + listOf(hidden, output)
        val projections = leabraProjections(network, layers, FEEDBACK_SCALE)
        LeabraTrainer(network, layers, projections, input, output).apply {
            learningRate = AX_OUTPUT_LEARNING_RATE
            expectedActivity[input] = 1.0 / AX_INPUTS.size
            stripes.forEach { expectedActivity[it] = 1.0 / AX_INPUTS.size }
            expectedActivity[output] = 1.0 / AX_OUTPUTS.size
        }
    }

    /**
     * Standard deviation of the noise added to Go minus NoGo, only while training.
     */
    var noise = 0.05
    var gatingLearningRate = 0.2
    var criticLearningRate = 0.1

    /**
     * The reward the critic expects.
     */
    var expectedReward by dopamine::expectedReward

    /**
     * Whether each stripe opened on the most recent step.
     */
    val gateOpen = BooleanArray(STRIPES)

    /**
     * The inputs shown since the last [clear].
     */
    val history = mutableListOf<Int>()

    private val tags = Array(STRIPES) { DoubleArray(input.size) }
    private var exploring = false

    /**
     * The input each stripe holds, if any.
     */
    fun held(stripe: Int): Int? = stripes[stripe].activationArray.indices.firstOrNull {
        stripes[stripe].activationArray[it] > 0.5
    }

    fun holds(needed: List<Int>) = needed.all { unit -> stripes.indices.any { held(it) == unit } }

    private fun decideGates(x: DoubleArray) {
        val acts = gatingWeights.weights.mv(x)
        basalGanglia.showActivations(acts)
        for (s in 0 until STRIPES) {
            val open = acts[2 * s + GO] - acts[2 * s + NOGO] + (if (exploring) noise * random.gaussian() else 0.0) > 0
            gateOpen[s] = open
            val sign = if (open) 1.0 else -1.0
            for (i in x.indices) tags[s][i] += sign * x[i]
        }
    }

    /**
     * Show [inputIndex], open or close each stripe's gate, and reset the rest of the network, ready for a minus
     * phase. Returns the trial.
     */
    fun startTrial(inputIndex: Int): AxTrial {
        history += inputIndex
        val x = oneHot(input.size, inputIndex)
        trainer.clamp(input, x)
        decideGates(x)
        stripes.forEachIndexed { s, m -> if (gateOpen[s]) trainer.clamp(m, x) else m.isClamped = true }
        setDopamine(0.0)
        trainer.startTrial(x)
        return axTrial(history)
    }

    /**
     * Run a step without learning and return the trial and the most active output unit.
     */
    fun test(inputIndex: Int, fireEvents: Boolean = false): Pair<AxTrial, Int> {
        val trial = startTrial(inputIndex)
        repeat(trainer.minusCycles) { trainer.cycle(fireEvents) }
        val acts = output.activationArray
        return trial to acts.indices.maxBy { acts[it] }
    }

    /**
     * Run a step with a minus and plus phase, learn, and on a rewarded step deliver dopamine.
     */
    fun train(inputIndex: Int, fireEvents: Boolean = false): AxStepResult {
        exploring = true
        val (trial, response) = try {
            test(inputIndex, fireEvents)
        } finally {
            exploring = false
        }
        val minus = trainer.snapshot()
        trainer.clamp(output, oneHot(output.size, trial.target))
        repeat(trainer.plusCycles) { trainer.cycle(fireEvents) }
        trainer.learn(minus, trainer.snapshot())
        val dopamine = if (trial.rewarded) reward(trial, learn = true) else null
        return AxStepResult(trial, response, dopamine)
    }

    /**
     * Reward for [trial], 1 if working memory holds what it needs, and 0 otherwise.
     */
    fun rewardFor(trial: AxTrial) = if (holds(trial.needed)) 1.0 else 0.0

    /**
     * Deliver reward for [trial] and return dopamine. If [learn], dopamine changes the tagged gating weights and the
     * expected reward. Tags are cleared either way.
     */
    fun reward(trial: AxTrial, learn: Boolean): Double {
        val da = rewardFor(trial) - expectedReward
        setDopamine(da)
        if (learn) {
            for (s in 0 until STRIPES) {
                learnGating(gatingWeights.weights, 2 * s + GO, 2 * s + NOGO, tags[s], da, gatingLearningRate)
            }
            gatingWeights.events.updated.fire()
            expectedReward += criticLearningRate * da
        }
        tags.forEach { it.fill(0.0) }
        return da
    }

    /**
     * Fraction of rewarded responses that were wrong, and of rewarded steps where working memory lacked what the
     * response needed, in the last [practice].
     */
    var wrongResponses = 0.0
        private set
    var memoryMissing = 0.0
        private set

    /**
     * Practice [count] sequences, each starting from empty working memory.
     */
    fun practice(count: Int, random: Random = this.random) {
        var rewarded = 0
        var wrong = 0
        var missing = 0
        repeat(count) {
            clear()
            for (inputIndex in axSequence(random)) {
                val result = train(inputIndex)
                val trial = result.trial
                if (trial.rewarded) {
                    rewarded++
                    if (!result.right) wrong++
                    if (!holds(trial.needed)) missing++
                }
            }
        }
        clear()
        wrongResponses = wrong.toDouble() / rewarded
        memoryMissing = missing.toDouble() / rewarded
    }

    /**
     * Empty working memory and the rest of the network, leaving weights alone.
     */
    fun clear() {
        (listOf(input) + stripes).forEach { trainer.clamp(it, DoubleArray(it.size)) }
        trainer.resetActivity()
        trainer.layers.forEach { it.events.updated.fire() }
        basalGanglia.showActivations(DoubleArray(basalGanglia.size))
        setDopamine(0.0)
        gateOpen.fill(false)
        tags.forEach { it.fill(0.0) }
        history.clear()
    }

    fun reset() {
        trainer.randomizeWeights()
        gatingWeights.randomize(initialGatingWeights())
        gatingWeights.events.updated.fire()
        expectedReward = 0.0
        clear()
    }

    private fun setDopamine(value: Double) = dopamine.showActivations(doubleArrayOf(value))
}

/**
 * Starting gating weights are nearly equal, so that exploration noise can flip any gate decision at first. With a
 * wider spread, some networks start with a decision strongly wrong, never hold what the answer needs, and so never
 * get the reward that would correct it.
 */
private fun initialGatingWeights() = UniformRealDistribution(0.45, 0.55)



/**
 * Draws a [GateLine] from each stripe's Go/NoGo pair to the stripe, behind the network, and returns them in stripe
 * order.
 */
suspend fun addGateLines(networkPanel: NetworkPanel, model: OneTwoAxModel): List<GateLine> {
    // TODO: The dashed gate lines still sometimes fail to show in the app, even with the full canvas repaint.
    val bg = networkPanel.modelNodeMap.get<NeuronArrayNode>(model.basalGanglia)
    val stripeNodes = model.stripes.map { networkPanel.modelNodeMap.get<NeuronArrayNode>(it) }
    return withContext(Dispatchers.Swing) {
        stripeNodes.mapIndexed { s, stripeNode ->
            GateLine(
                listOf(bg.neuronCircles[2 * s + GO], bg.neuronCircles[2 * s + NOGO]),
                stripeNode.neuronCircles,
                listOf(bg, stripeNode)
            ).also { networkPanel.canvas.layer.addChild(0, it) }
        }
    }
}

/**
 * Draws an [AxSequenceStrip] lined up with the left edge of the first stripe and the top of the output layer, and
 * returns it.
 */
suspend fun addSequenceStrip(networkPanel: NetworkPanel, model: OneTwoAxModel): AxSequenceStrip {
    val stripeNode = networkPanel.modelNodeMap.get<NeuronArrayNode>(model.stripes.first())
    val outputNode = networkPanel.modelNodeMap.get<NeuronArrayNode>(model.output)
    return withContext(Dispatchers.Swing) {
        AxSequenceStrip(stripeNode, outputNode).also { networkPanel.canvas.layer.addChild(it) }
    }
}

/**
 * Draws a [DopamineBadge] above the dopamine unit and returns it.
 */
suspend fun addDopamineBadge(networkPanel: NetworkPanel, model: OneTwoAxModel): DopamineBadge {
    val dopamineNode = networkPanel.modelNodeMap.get<NeuronArrayNode>(model.dopamine)
    return withContext(Dispatchers.Swing) {
        DopamineBadge(dopamineNode).also { networkPanel.canvas.layer.addChild(it) }
    }
}

const val STRIPES = 2
fun stripeLabel(stripe: Int) = "Working memory ${stripe + 1} (PFC)"
const val AX_INPUT = "Input"
const val AX_HIDDEN = "Hidden"
const val AX_OUTPUT = "Output"
private const val AX_HIDDEN_SIZE = 40
private const val AX_OUTPUT_LEARNING_RATE = 0.03
private const val AX_WEIGHTS_LABEL = "Weights"
private const val AX_PLOT = "Mistakes"
private const val AX_RESPONSE_SERIES = "Wrong responses (%)"
private const val AX_MEMORY_SERIES = "Memory missing digit or letter (%)"
private const val AX_PRACTICE_SEQUENCES = 100
private const val AX_PLOT_BLOCK = 10
private const val SLOW_STEP_MILLIS = 1000L

private suspend fun SimulationScope.setUpOneTwoAx(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val model = OneTwoAxModel(network)
    val steps = mutableListOf<AxStepView>()
    var strip: AxSequenceStrip? = null
    var flashGatingWeights: suspend () -> Unit = {}
    var badge: DopamineBadge? = null
    var gateLines: List<GateLine> = emptyList()

    /**
     * Repaints the whole network canvas after an overlay changes, since a repaint of the overlay's own area does not
     * always reach the screen.
     */
    var repaintCanvas: () -> Unit = {}

    // TODO: In the app the badge has only ever shown NO DOPAMINE CHANGE, even during slow practice on a new network.
    // Check whether bursts and dips reach it, or are just rarer than expected in the first sequences.
    fun showBadge(da: Double?) {
        val change = da?.let {
            when {
                abs(it) < DOPAMINE_THRESHOLD -> DopamineChange.NONE
                it > 0 -> DopamineChange.BURST
                else -> DopamineChange.DIP
            }
        }
        badge?.let {
            SwingUtilities.invokeLater {
                it.change = change
                repaintCanvas()
            }
        }
    }
    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == AX_PLOT }
    var sequences = AX_PLOT_BLOCK * (plot.model.timeSeriesList.firstOrNull()?.series?.itemCount ?: 0)

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Leabra cycle") { model.trainer.cycle() })

    fun showGates() {
        val open = model.gateOpen.toList()
        val lines = gateLines
        SwingUtilities.invokeLater {
            lines.forEachIndexed { s, line -> line.open = open[s] }
            repaintCanvas()
        }
    }

    fun showSteps() {
        val shown = steps.toList()
        strip?.let {
            SwingUtilities.invokeLater {
                it.steps = shown
                repaintCanvas()
            }
        }
    }

    fun showCleared() {
        showGates()
        showBadge(null)
        steps.clear()
        showSteps()
    }

    suspend fun runStep(inputIndex: Int) {
        workspace.stop()
        val trial = model.startTrial(inputIndex)
        showGates()
        showBadge(null)
        steps += AxStepView(inputIndex, trial.target, null)
        showSteps()
        workspace.iterateSuspend(model.trainer.minusCycles)
        val acts = model.output.activationArray
        steps[steps.lastIndex] = steps.last().copy(response = acts.indices.maxBy { acts[it] })
        showSteps()
        if (trial.rewarded) {
            showBadge(model.reward(trial, learn = false))
        }
    }

    suspend fun practiceSlowly() {
        workspace.stop()
        model.clear()
        showCleared()
        for (inputIndex in axSequence(model.random)) {
            val result = model.train(inputIndex, fireEvents = true)
            showGates()
            steps += AxStepView(inputIndex, result.trial.target, result.response)
            showSteps()
            val da = result.dopamine
            showBadge(da)
            if (da != null && abs(da) >= DOPAMINE_THRESHOLD) flashGatingWeights()
            delay(SLOW_STEP_MILLIS)
        }
    }

    fun practice(count: Int) {
        workspace.stop()
        repeat(count / AX_PLOT_BLOCK) {
            model.practice(AX_PLOT_BLOCK)
            sequences += AX_PLOT_BLOCK
            plot.model.addData(0, sequences.toDouble(), 100 * model.wrongResponses)
            plot.model.addData(1, sequences.toDouble(), 100 * model.memoryMissing)
        }
        showCleared()
    }

    withGui {
        val networkPanel = getNetworkPanel(networkComponent)
        strip = addSequenceStrip(networkPanel, model)
        repaintCanvas = { networkPanel.canvas.repaint() }
        badge = addDopamineBadge(networkPanel, model)
        gateLines = addGateLines(networkPanel, model)
        val gatingNode = networkPanel.modelNodeMap.get<WeightMatrixNode>(model.gatingWeights)
        flashGatingWeights = { flashWeights(networkPanel, listOf(gatingNode)) }
        val controlPanel = createControlPanel("1-2-AX", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            for ((rowLabel, range) in listOf("Digit" to DIGITS, "First letter" to FIRST_LETTERS, "Second letter" to SECOND_LETTERS)) {
                addRow(rowLabel) {
                    for (i in range) {
                        addButton(AX_INPUTS[i]) { runStep(i) }.apply {
                            toolTipText = "Show ${AX_INPUTS[i]}. Each stripe's basal ganglia decide whether to store it."
                        }
                    }
                }
            }
            addButton("Clear memory") {
                workspace.stop()
                model.clear()
                showCleared()
            }.apply { toolTipText = "Empty both stripes, ready for a new sequence." }
            addSeparator()
            addButton("Practice 1 sequence slowly") {
                val label = text
                withContext(Dispatchers.Swing) {
                    isEnabled = false
                    text = "Practicing…"
                }
                try {
                    practiceSlowly()
                } finally {
                    withContext(NonCancellable + Dispatchers.Swing) {
                        text = label
                        isEnabled = true
                    }
                }
            }.apply {
                toolTipText = "Practice on one random sequence, a step at a time, learning as it goes. The " +
                    "gating weights flash when dopamine changes them."
            }
            for (count in listOf(AX_PLOT_BLOCK, AX_PRACTICE_SEQUENCES)) {
                addButton("Practice $count sequences") { practice(count) }.apply {
                    toolTipText = "Practice on $count random sequences, learning as it goes, and plot the mistakes."
                }
            }
            addSeparator()
            addButton("Reset") {
                workspace.stop()
                model.reset()
                showCleared()
                sequences = 0
                plot.model.clearData()
            }.apply { toolTipText = "Randomize the weights, empty memory, reset expected reward, and clear the plot." }
        }.awaitLayout()
        val networkX = controlPanel.rightEdgeWithGap()
        place(networkComponent, networkX, SIM_WINDOW_GAP, 640, 640)
        place(plot, networkX + 640 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
    }
}

private const val AX_SIDEBAR = """
# 1-2-AX

Digits and letters are shown one at a time, and after each one the network answers L or R. The right answer
depends on what came before, so [working memory](https://en.wikipedia.org/wiki/Working_memory) has to hold the last
digit for a long time while also holding the latest letter, which changes with every pair. The task comes from
experiments where people watch the sequence on a screen and press a left (L) or right (R) button.

**The rule**

- R for `X` right after `A`, when the last digit was `1`
- R for `Y` right after `B`, when the last digit was `2`
- L for everything else

Here you build the sequences with the control panel: click `1` or `2`, then alternate between the `A` `B` `C` row
and the `X` `Y` `Z` row for as long as you like. The network answers L or R on each step, and with practice it
learns to answer correctly.

This simulation illustrates three principles:

1. Working memory, in [prefrontal cortex](https://en.wikipedia.org/wiki/Prefrontal_cortex) (PFC), is divided into
   stripes, each with its own gate. One stripe can hold the digit while the other
   takes in each new letter.
2. Each stripe has its own `Go` and `NoGo` units in the [basal ganglia](https://en.wikipedia.org/wiki/Basal_ganglia)
   (BG), so each learns its own rule for when to open.
3. One [dopamine](https://en.wikipedia.org/wiki/Dopamine) signal reaches every stripe. Because each stripe tags its
   own decisions, the same dopamine can teach the stripes different roles.

This simulation builds on the `Learned gating` simulation, earlier in this Leabra series, where one stripe learned
when to open from dopamine. What it adds is a second stripe and a task that needs both.

# Simulation Details

The buttons in the top three rows of the control panel, `Digit`, `First letter`, and `Second letter`, are the inputs
to the network: each click shows it one input. On each step, `Go` and `NoGo` for each stripe in `Basal ganglia (BG)`
are driven by `Input` through the `Gating weights`. If `Go` beats `NoGo`, that stripe takes in the input; otherwise
it keeps what it holds. A dashed line runs from each stripe's `Go` and `NoGo` up to the stripe. It stands for the
loop through the thalamus by which the basal ganglia gate the stripe, and it turns solid and red when the gate
opens. `Output` answers L or R on every step, using the input and both stripes through `Hidden`, and learns from the
correct answer as in the earlier simulations.

At the top left of the network, a strip has one box per step since memory was last cleared, showing the input, the correct
answer, and the network's answer (its most active `Output` unit). A box is outlined in green with a ✓ when the
network was right and in red with an ✗ when it was wrong. The newest box has the thickest outline.

## Possible sequences

A sequence is a digit followed by one to four letter pairs. Each pair is `A`, `B`, or `C` followed by `X`, `Y`, or
`Z`, so there are 9 possible pairs. Each pair is judged using the most recent digit. The answer to the digit and to
each first letter is always L; the answer shown here is for the second letter of each pair.

With one pair:

- 1 A X → R
- 1 B Y → L
- 1 C X → L
- 2 B Y → R
- 2 A X → L
- 2 B Z → L

With several pairs:

- 1 C Z, B Y, A X → L, L, R
- 2 A X, B Y → L, R
- 1 A Y, A X, B X → L, R, L

The `Practice` buttons make random sequences, but not uniformly random. A pair like `A X` is a target after a 1 and
not after a 2, so practice includes many of both: about 1 pair in 4 is the target for the current digit, 1 in 4 is
the target for the other digit, and the rest are other pairs. That way, neither the digit nor the pair alone gives
the answer.

Dopamine is released on the second letter of each pair (X, Y, or Z). As in the `Learned gating` simulation, it is
the reward minus the reward expected, and it strengthens or weakens the tagged choices of both stripes. A yellow
On each of these steps a badge above `Dopamine (SNc)` says what dopamine did: a yellow DOPAMINE BURST when memory held
what was needed more often than expected, a blue DOPAMINE DIP when less often, and a gray NO DOPAMINE CHANGE when the
result was close to what was expected. A burst strengthens the gate choices that led there and a dip weakens them;
with no change, the weights change only a little. During
`Practice 1 sequence slowly`, the `Gating weights` flash whenever dopamine changes them.

## What is rewarded

In PBWM, and in the `Learned gating` simulation, the reward is a right answer. Here the reward is 1 if working
memory holds what the answer needs, the digit and the first letter of the pair, and 0 otherwise.

The reason is a problem the `Learned gating` simulation already showed: the gates and the rest of the network must
learn together. There the answer depended only on working memory, so a little progress in one helped the other. Here
the answer depends on three things at once: the digit, the first letter, and the current letter. The rest of the
network cannot learn that combination until the gates are almost always right, and the gates get no useful dopamine
until the rest of the network can use what they store. In our tests, networks rewarded for right answers never got
past answering L to everything, which is right three times out of four. Full PBWM gets past this with more
machinery, including a critic that learns which working memory states lead to reward, and with much longer training.

The plot shows two kinds of mistakes on second letters in each block of 10 sequences: wrong answers, and steps
where working memory was missing the digit or the first letter.

## Control Panel Settings

- `Digit`, `First letter`, `Second letter`: Show that input. Each stripe's BG decide whether to store it.
- `Clear memory`: Empty both stripes, ready for a new sequence. What the network has learned is kept.
- `Practice 1 sequence slowly`: Practice on one random sequence a step at a time, learning as it goes, so you can
  watch the gates and dopamine. While it runs, the button reads `Practicing…` and cannot be clicked.
- `Practice $AX_PLOT_BLOCK sequences`, `Practice $AX_PRACTICE_SEQUENCES sequences`: Practice on random sequences,
  learning as it goes, and plot the mistakes.
- `Reset`: Randomize all the weights, empty memory, reset the expected reward, and clear the plot, so the network
  forgets everything it learned.

Only the `Practice` buttons learn.

# Activities

## Activity 1: An untrained network fails
1. Click `1`, `A`, `X`.
    - The correct answers, shown in the strip's `Correct` row, are L, L, R: R because `X` came right after `A` and
      the last digit was 1.
    - An untrained network gets the R only by chance. Its gates have not learned what to keep, so working memory
      rarely still holds both `1` and `A` when `X` arrives.
2. Click `Clear memory`, then `2`, `A`, `X`.
    - The same letters, but now the correct answers are L, L, L, because after a 2 the target is `B` then `Y`. So
      the network cannot answer from the letters alone.

## Activity 2: Watch dopamine train the gates
1. Click `Practice 1 sequence slowly`.
    - Watch the strip fill in and the gate lines turn solid and dashed. During practice the gates sometimes try the
      other choice, which is how they explore.
    - On each second letter, dopamine compares what memory held with what was expected. A new network usually gets
      no reward and expects none, so the badge usually says NO DOPAMINE CHANGE.
2. Click `Practice $AX_PLOT_BLOCK sequences`, then `Practice 1 sequence slowly` again. Repeat until you see dopamine
   bursts and dips, which usually takes a few rounds.
    - A burst means memory held the digit and the first letter more often than the network expected. The
      `Gating weights` flash as dopamine strengthens the gate choices that led there. A dip weakens them.
3. Keep alternating for a few more rounds.
    - As memory gets it right more often, the network comes to expect the reward, and bursts become rarer and
      smaller.
4. Click `Practice $AX_PRACTICE_SEQUENCES sequences` until both lines in the plot fall to near 0.
    - Working memory usually improves first, then the answers.

## Activity 3: Test the trained network
1. Click `Clear memory`, then `1`, `A`, `X`.
    - One stripe stores the digit and keeps it. The other stores `A` and keeps it through `X`. The network answers
      R, and the strip shows a ✓. Memory held `1` and `A`, as expected, so there is no dopamine change.
2. Continue with `B`, `Y`, then `A`, `X`.
    - The digit stays put while the other stripe takes each new first letter. The network answers L to `Y` and R to
      the second `X`. In a well-trained network, every box in the strip has a ✓.
3. Click `Clear memory`, then `2`, `A`, `X`.
    - Same letters, different digit, so the network answers L.
4. Click `Practice 1 sequence slowly`.
    - The badge mostly says NO DOPAMINE CHANGE, and the `Gating weights` rarely flash: memory holds what is needed, as expected, so
      there is little left to learn.
5. Look at the `Gating weights`.
    - One stripe's `Go` is strong for the digits, the other's for `A`, `B`, and `C`. Which stripe takes which role
      differs from network to network.

## Explore further
- Reset and practice again. Do the stripes swap roles?
- Why must the stripe that holds the first letter stay closed on `X`, `Y`, and `Z`?

# References

O'Reilly, R. C., & Frank, M. J. (2006). Making working memory work: A computational model of learning in the
prefrontal cortex and basal ganglia. _Neural Computation, 18_(2), 283–328.

O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
[_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
"""
