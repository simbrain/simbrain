/**
 * An interactive, compact implementation of Botvinick and Plaut's activation-based recurrent account of
 * immediate serial recall. Its specialised teacher-forced trainer belongs here so general BPTT networks
 * retain their existing semantics.
 */
package org.simbrain.custom_sims.simulations.psychology

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.format
import org.simbrain.util.place
import org.simbrain.util.point
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

val botvinickPlautSerialRecall = newSim {

    workspace.clearWorkspace()

    val model = BotvinickPlautModel(random = Random(12))
    val networkComponent = addNetworkComponent("Botvinick-Plaut Serial Recall")
    val network = networkComponent.network

    val input = NeuronArray(model.inputSize).apply {
        label = "Input"
        labelArray = (model.itemLabels + "Recall cue").toTypedArray()
        updateRule = LinearRule().apply {
            lowerBound = 0.0
            upperBound = 1.0
        }
        isClamped = true
        circleMode = true
        location = point(720.0, 560.0)
    }
    val hidden = NeuronArray(model.hiddenSize).apply {
        label = "Hidden (distributed memory)"
        updateRule = SigmoidalRule()
        circleMode = true
        gridMode = true
        location = point(300.0, 260.0)
    }
    val output = NeuronArray(model.outputSize).apply {
        label = "Output"
        updateRule = SoftmaxRule()
        labelArray = (model.itemLabels + "End").toTypedArray()
        circleMode = true
        location = point(720.0, 40.0)
    }
    val inputToHidden = WeightMatrix(input, hidden).apply { label = "Input to hidden" }
    val hiddenToHidden = WeightMatrix(hidden, hidden).apply { label = "Hidden recurrence" }
    val hiddenToOutput = WeightMatrix(hidden, output).apply { label = "Hidden to output" }
    val outputToHidden = WeightMatrix(output, hidden).apply { label = "Output feedback" }
    network.addNetworkModels(input, hidden, output, inputToHidden, hiddenToHidden, hiddenToOutput, outputToHidden)
    hidden.location = point(300.0, 260.0)
    input.location = point(720.0, 560.0)
    output.location = point(720.0, 40.0)

    fun syncModel() {
        input.setActivations(model.currentInput)
        hidden.setActivations(model.hiddenState)
        output.setActivations(model.outputState)
        model.copyWeightsTo(inputToHidden, hiddenToHidden, hiddenToOutput, outputToHidden)
    }

    var listLength = 4
    var trial = model.newTrial(listLength)
    var step = 0
    val recalled = mutableListOf<Int>()

    fun resetTrial(length: Int = listLength) {
        trial = model.newTrial(length)
        step = 0
        recalled.clear()
        model.resetState()
        syncModel()
    }

    fun advanceTrial() {
        if (step >= trial.inputs.size) return
        val inputVector = trial.inputs[step]
        model.forward(inputVector, if (step == 0) DoubleArray(model.outputSize) else model.outputState)
        if (step >= trial.items.size && step < trial.items.size * 2) {
            recalled += model.outputState.argmax(model.itemLabels.size)
        }
        step++
        syncModel()
    }

    fun runTrial() {
        resetTrial()
        while (step < trial.inputs.size) advanceTrial()
    }

    fun sequenceText(items: List<Int>) = if (items.isEmpty()) "(none)" else items.joinToString(" ") { model.itemLabels[it] }

    withGui {
        var trainingFrame: JFrame? = null
        var updateTrainingSummary: (String) -> Unit = {}

        fun showTrainingFrame() {
            trainingFrame?.takeIf { it.isDisplayable }?.let {
                it.toFront()
                return
            }
            val cycles = JSpinner(SpinnerNumberModel(12_000, 1, 200_000, 1_000))
            val errorText = JLabel("Cross entropy: ${if (model.trainingCycles == 0) "not yet trained" else model.lastLoss.format(4)}")
            val progressText = JLabel("${model.trainingCycles} completed cycles")
            val trainButton = JButton("Train")
            val resetButton = JButton("Reset weights")
            val errorModel = TimeSeriesModel().apply { addTimeSeries("Cross entropy") }
            val errorPanel = TimeSeriesPlotPanel(errorModel).apply {
                preferredSize = Dimension(460, 260)
                seriesRemovalEnabled = false
                chartPanel.chart.xyPlot.domainAxis.label = "Training cycle"
                chartPanel.chart.xyPlot.rangeAxis.label = "Cross entropy"
            }
            val controls = JPanel(GridLayout(0, 2, 8, 8)).apply {
                border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
                add(JLabel("Training cycles"))
                add(cycles)
                add(errorText)
                add(progressText)
                add(trainButton)
                add(resetButton)
            }
            val frame = JFrame("Serial Recall Training").apply {
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                layout = BorderLayout()
                add(controls, BorderLayout.NORTH)
                add(errorPanel, BorderLayout.CENTER)
                pack()
                setLocationRelativeTo(null)
            }
            trainingFrame = frame
            trainButton.addActionListener {
                val requestedCycles = cycles.value as Int
                trainButton.isEnabled = false
                resetButton.isEnabled = false
                progressText.text = "Training $requestedCycles cycles..."
                object : SwingWorker<Unit, Pair<Int, Double>>() {
                    override fun doInBackground() {
                        repeat(requestedCycles) { cycle ->
                            model.trainCycle()
                            if (cycle % 100 == 99 || cycle == requestedCycles - 1) publish(model.trainingCycles to model.lastLoss)
                        }
                    }

                    override fun process(samples: MutableList<Pair<Int, Double>>) {
                        samples.forEach { (cycle, error) ->
                            errorModel.timeSeriesList[0].series.add(cycle, error)
                        }
                        errorText.text = "Cross entropy: ${samples.last().second.format(4)}"
                    }

                    override fun done() {
                        syncModel()
                        val summary = "${model.trainingCycles} cycles, loss ${model.lastLoss.format(3)}"
                        errorText.text = "Cross entropy: ${model.lastLoss.format(4)}"
                        progressText.text = summary
                        updateTrainingSummary(summary)
                        trainButton.isEnabled = true
                        resetButton.isEnabled = true
                    }
                }.execute()
            }
            resetButton.addActionListener {
                model.randomizeWeights()
                resetTrial()
                errorModel.clearData()
                errorText.text = "Cross entropy: not yet trained"
                progressText.text = "weights reset"
                updateTrainingSummary("weights reset")
            }
            frame.isVisible = true
        }

        val controlPanel = createControlPanel("Botvinick-Plaut Serial Recall", 10, 10) {
            val targetText = addLabelledText("Target", sequenceText(trial.items))
            val recalledText = addLabelledText("Recalled", sequenceText(recalled))
            val phaseText = addLabelledText("Phase", "Encoding")
            val accuracyText = addLabelledText("Trial accuracy", "0.0%")
            val trainingText = addLabelledText("Training", "not yet trained")
            updateTrainingSummary = { trainingText.text = it }

            fun refresh() {
                targetText.text = sequenceText(trial.items)
                recalledText.text = sequenceText(recalled)
                phaseText.text = when {
                    step >= trial.inputs.size -> "Done"
                    step < trial.items.size -> "Encoding"
                    else -> "Recall"
                }
                val accuracy = trial.items.zip(recalled).count { it.first == it.second }.toDouble() / trial.items.size
                accuracyText.text = "${(100.0 * accuracy).format(1)}%"
            }

            addComboBox("List length", (1..model.maxListLength).toList(), listLength) {
                listLength = it
                resetTrial()
                refresh()
            }.apply { toolTipText = "Choose the length of the next displayed serial-recall trial." }
            addSeparator()
            addButton("New Trial") {
                resetTrial()
                refresh()
            }.apply { toolTipText = "Draw a fresh list and reset the recurrent activation state." }
            addButton("Step") {
                advanceTrial()
                refresh()
            }.apply { toolTipText = "Advance one encoding or recall timestep using frozen weights." }
            addButton("Run Trial") {
                runTrial()
                refresh()
            }.apply { toolTipText = "Run a complete frozen-weight serial-recall trial." }
            addSeparator()
            addButton("Training...", context = Dispatchers.Swing) { showTrainingFrame() }
                .apply { toolTipText = "Open the training window with cycle count and cross-entropy feedback." }
            addButton("Evaluate", context = Dispatchers.Swing) {
                val evaluation = model.evaluate(trialsPerLength = 40)
                showEvaluationWindow(evaluation, model.maxListLength)
                trainingText.text = "frozen test: mean ${(100 * evaluation.wholeListAccuracy.average()).format(1)}%"
                syncModel()
            }.apply { toolTipText = "Test frozen weights and plot list-length, position, and transposition results." }
            refresh()
        }.awaitLayout()

        controlPanel.setBounds(10, 10, 264, 500)
        place(networkComponent, 286, 8, 1050, 660)
    }

    syncModel()

    addSidebarInfo(
        """
        # Botvinick-Plaut Serial Recall

        This simulation is a compact implementation of Botvinick and Plaut's recurrent account of immediate serial recall. Unlike the nearby toy model, it learns its recurrent dynamics from sequences using backpropagation through time. During testing, weights are frozen: the list is maintained only as a distributed pattern of hidden-unit activation.

        # Control Panel

        `List length` chooses how many items appear in the displayed trial. It resets the trial but does not alter the model's training curriculum, which includes every length from one through six.

        `New Trial` draws a fresh list at the selected length, clears the recurrent activation state, and prepares the first encoding step. It does not change learned weights.

        `Step` advances one timestep. During encoding, it presents one item and the output echoes it; after the final item, it switches to recall-cue-only input and returns one remembered item per step, followed by `End`.

        `Run Trial` resets to a fresh target list and completes all encoding and recall steps at once, using frozen weights.

        `Training...` opens a separate training window. Set `Training cycles` to control how much experience the model receives; `Train` shows live cross-entropy, and `Reset weights` discards all learned structure so you can compare an untrained or briefly trained model with a well-trained one.

        `Evaluate` tests the frozen network on 40 new lists of each length and opens a separate results window. It does not train the model or alter its weights.

        ## Reading the Evaluation

        **Whole-list accuracy** is the proportion of lists recalled perfectly at each list length. Longer lists are usually harder, so accuracy normally falls as length increases.

        **Serial-position accuracy** asks how often the item originally shown in each position is recalled in that same position. People tend to remember early and late items in a list better than middle items; this is called *primacy* and *recency*. A trained model may show a related pattern, with a higher beginning and/or end of the curve.

        A **transposition error** is a recall-order error: an item is remembered but appears in the wrong position. For example, recalling `A C B D` after seeing `A B C D` exchanges the items from positions 2 and 3. The transposition plot groups these errors by how far an item moved. An item shown in position 2 but recalled in position 3 is a one-position error; an item shown in position 2 but recalled in position 5 is a three-position error. More frequency near the middle of this plot means that nearby positions are more often confused than distant positions.

        # What to Do

        ## Test Recall Before and After Training

        1. Choose a list length, click `New Trial`, and use `Step` to walk through the trial. Before training, the network recalls poorly: its hidden activity has no learned structure for maintaining and reading out the list.
        2. Click `Training...`, leave `Training cycles` at the default 12,000, and click `Train`. Watch cross-entropy fall in the training window.
        3. Click `New Trial` and use `Step` again. The output still echoes items during encoding, but after the recall cue it should now reproduce the remembered list and then `End` much more accurately.
        4. Try shorter training runs after `Reset weights` to see how incomplete learning produces worse recall, especially later in a list.

        ## Evaluate Aggregate Recall

        1. Click `Evaluate` before training. The untrained network's three plots are poor: whole-list and serial-position accuracy are near chance, and its recall-order error pattern is not meaningful.
        2. After the default training run, click `Evaluate` again. Compare the replacement plots: whole-list accuracy should improve, serial-position accuracy should rise, and the transposition distribution now describes the trained model's characteristic recall-order errors.

        # Model Details

        The network has input-to-hidden, hidden-to-hidden, hidden-to-output, and output-to-hidden connections. During training, output feedback uses the correct preceding target (teacher forcing). During trial playback and evaluation, it instead uses the network's own preceding output. This teacher-forced training logic is specific to this simulation and does not alter Simbrain's general BPTT networks.

        The original paper used 26 letters, 200 hidden units, and much longer training. This interactive version uses six items and 40 hidden units but preserves the architecture, task structure, softmax competition, teacher forcing, and frozen-weight testing distinction.

        # Reference

        Botvinick, M. M., & Plaut, D. C. (2006). [_Short-term memory for serial order: A recurrent neural network model_](https://doi.org/10.1037/0033-295X.113.2.201). _Psychological Review_, _113_(2), 201-233.
        """.trimIndent(),
        width = 390
    )
}

internal class BotvinickPlautModel(private val random: Random) {
    val itemLabels = listOf("A", "B", "C", "D", "E", "F")
    val maxListLength = itemLabels.size
    val inputSize = itemLabels.size + 1
    val outputSize = itemLabels.size + 1
    val hiddenSize = 40
    private val learningRate = 0.1

    private val inputToHidden = Array(hiddenSize) { DoubleArray(inputSize) }
    private val hiddenToHidden = Array(hiddenSize) { DoubleArray(hiddenSize) }
    private val hiddenToOutput = Array(outputSize) { DoubleArray(hiddenSize) }
    private val outputToHidden = Array(hiddenSize) { DoubleArray(outputSize) }
    private val hiddenBias = DoubleArray(hiddenSize)
    private val outputBias = DoubleArray(outputSize)

    var hiddenState = DoubleArray(hiddenSize) { 0.5 }
        private set
    var outputState = DoubleArray(outputSize)
        private set
    var currentInput = DoubleArray(inputSize)
        private set
    var trainingCycles = 0
        private set
    var lastLoss = 0.0
        private set

    init {
        randomizeWeights()
    }

    fun randomizeWeights() {
        fun randomize(matrix: Array<DoubleArray>, scale: Double) = matrix.forEach { row ->
            row.indices.forEach { row[it] = random.nextDouble(-scale, scale) }
        }
        randomize(inputToHidden, 0.3)
        randomize(hiddenToHidden, 0.15)
        randomize(hiddenToOutput, 0.3)
        randomize(outputToHidden, 0.3)
        hiddenBias.fill(-1.0)
        outputBias.fill(0.0)
        trainingCycles = 0
        lastLoss = 0.0
        resetState()
    }

    fun resetState() {
        hiddenState = DoubleArray(hiddenSize) { 0.5 }
        outputState = DoubleArray(outputSize)
        currentInput = DoubleArray(inputSize)
    }

    fun newTrial(length: Int) = SerialRecallTrial(
        items = itemLabels.indices.shuffled(random).take(length),
        inputSize = inputSize,
        outputSize = outputSize
    )

    fun forward(input: DoubleArray, feedback: DoubleArray): DoubleArray {
        val nextHidden = DoubleArray(hiddenSize) { h -> sigmoid(
            hiddenBias[h] + inputToHidden[h].dot(input) + hiddenToHidden[h].dot(hiddenState) + outputToHidden[h].dot(feedback)
        ) }
        val logits = DoubleArray(outputSize) { o -> outputBias[o] + hiddenToOutput[o].dot(nextHidden) }
        currentInput = input.copyOf()
        hiddenState = nextHidden
        outputState = logits.softmax()
        return outputState
    }

    fun trainCycle() {
        (1..maxListLength).forEach { trainTrial(newTrial(it)) }
        trainingCycles++
    }

    private fun trainTrial(trial: SerialRecallTrial) {
        val states = mutableListOf<TrainingState>()
        resetState()
        var forcedFeedback = DoubleArray(outputSize)
        trial.inputs.indices.forEach { timestep ->
            val previousHidden = hiddenState.copyOf()
            val output = forward(trial.inputs[timestep], forcedFeedback)
            states += TrainingState(trial.inputs[timestep], trial.targets[timestep], previousHidden, forcedFeedback, hiddenState.copyOf(), output)
            forcedFeedback = trial.targets[timestep]
        }
        lastLoss = states.sumOf { state -> -state.target.indices.sumOf { index -> state.target[index] * ln(state.output[index].coerceAtLeast(1e-12)) } } / states.size
        val dInputToHidden = Array(hiddenSize) { DoubleArray(inputSize) }
        val dHiddenToHidden = Array(hiddenSize) { DoubleArray(hiddenSize) }
        val dHiddenToOutput = Array(outputSize) { DoubleArray(hiddenSize) }
        val dOutputToHidden = Array(hiddenSize) { DoubleArray(outputSize) }
        val dHiddenBias = DoubleArray(hiddenSize)
        val dOutputBias = DoubleArray(outputSize)
        var futureHiddenError = DoubleArray(hiddenSize)
        for (timestep in states.lastIndex downTo 0) {
            val state = states[timestep]
            val outputError = DoubleArray(outputSize) { state.output[it] - state.target[it] }
            val hiddenError = DoubleArray(hiddenSize) { h ->
                (hiddenToOutput.indices.sumOf { o -> hiddenToOutput[o][h] * outputError[o] } + futureHiddenError[h]) * state.hidden[h] * (1.0 - state.hidden[h])
            }
            outputError.indices.forEach { o ->
                dOutputBias[o] += outputError[o]
                state.hidden.indices.forEach { h -> dHiddenToOutput[o][h] += outputError[o] * state.hidden[h] }
            }
            hiddenError.indices.forEach { h ->
                dHiddenBias[h] += hiddenError[h]
                state.input.indices.forEach { i -> dInputToHidden[h][i] += hiddenError[h] * state.input[i] }
                state.previousHidden.indices.forEach { p -> dHiddenToHidden[h][p] += hiddenError[h] * state.previousHidden[p] }
                state.forcedFeedback.indices.forEach { o -> dOutputToHidden[h][o] += hiddenError[h] * state.forcedFeedback[o] }
            }
            futureHiddenError = DoubleArray(hiddenSize) { p -> hiddenToHidden.indices.sumOf { h -> hiddenToHidden[h][p] * hiddenError[h] } }
        }
        val scale = learningRate / states.size
        inputToHidden.applyGradient(dInputToHidden, scale)
        hiddenToHidden.applyGradient(dHiddenToHidden, scale)
        hiddenToOutput.applyGradient(dHiddenToOutput, scale)
        outputToHidden.applyGradient(dOutputToHidden, scale)
        hiddenBias.indices.forEach { hiddenBias[it] -= scale * dHiddenBias[it] }
        outputBias.indices.forEach { outputBias[it] -= scale * dOutputBias[it] }
    }

    fun evaluate(trialsPerLength: Int): RecallEvaluation {
        val wholeList = DoubleArray(maxListLength)
        val serialCorrect = DoubleArray(maxListLength)
        val serialTotal = DoubleArray(maxListLength)
        val distanceCounts = DoubleArray(maxListLength * 2 + 1)
        (1..maxListLength).forEach { length ->
            repeat(trialsPerLength) {
                val trial = newTrial(length)
                resetState()
                val responses = mutableListOf<Int>()
                trial.inputs.indices.forEach { timestep ->
                    forward(trial.inputs[timestep], if (timestep == 0) DoubleArray(outputSize) else outputState)
                    if (timestep in length until length * 2) responses += outputState.argmax(itemLabels.size)
                }
                if (responses == trial.items) wholeList[length - 1] += 1.0
                trial.items.indices.forEach { position ->
                    serialTotal[position]++
                    if (responses[position] == trial.items[position]) serialCorrect[position]++
                    else {
                        val sourcePosition = trial.items.indexOf(responses[position])
                        if (sourcePosition >= 0) distanceCounts[sourcePosition - position + maxListLength]++
                    }
                }
            }
        }
        return RecallEvaluation(
            wholeList.map { it / trialsPerLength }.toDoubleArray(),
            serialCorrect.indices.associateWith { index -> if (serialTotal[index] == 0.0) null else serialCorrect[index] / serialTotal[index] },
            distanceCounts.normalize()
        )
    }

    fun copyWeightsTo(inputToHiddenView: WeightMatrix, hiddenToHiddenView: WeightMatrix, hiddenToOutputView: WeightMatrix, outputToHiddenView: WeightMatrix) {
        inputToHiddenView.weights.copyValuesFrom(inputToHidden)
        hiddenToHiddenView.weights.copyValuesFrom(hiddenToHidden)
        hiddenToOutputView.weights.copyValuesFrom(hiddenToOutput)
        outputToHiddenView.weights.copyValuesFrom(outputToHidden)
    }

    internal fun weightChecksum() = inputToHidden.sumOf { it.sum() } + hiddenToHidden.sumOf { it.sum() } +
            hiddenToOutput.sumOf { it.sum() } + outputToHidden.sumOf { it.sum() } + hiddenBias.sum() + outputBias.sum()
}

internal data class SerialRecallTrial(val items: List<Int>, val inputs: List<DoubleArray>, val targets: List<DoubleArray>) {
    constructor(items: List<Int>, inputSize: Int, outputSize: Int) : this(
        items,
        items.map { oneHot(inputSize, it) } + List(items.size + 1) { oneHot(inputSize, inputSize - 1) },
        items.map { oneHot(outputSize, it) } + items.map { oneHot(outputSize, it) } + listOf(oneHot(outputSize, outputSize - 1))
    )
}

private data class TrainingState(val input: DoubleArray, val target: DoubleArray, val previousHidden: DoubleArray, val forcedFeedback: DoubleArray, val hidden: DoubleArray, val output: DoubleArray)

internal data class RecallEvaluation(val wholeListAccuracy: DoubleArray, val serialPositionAccuracy: Map<Int, Double?>, val transpositionDistance: DoubleArray)

private fun oneHot(size: Int, index: Int) = DoubleArray(size).also { it[index] = 1.0 }

private fun showEvaluationWindow(evaluation: RecallEvaluation, maxListLength: Int) {
    fun plot(title: String, xAxis: String, yAxis: String, points: List<Pair<Int, Double>>) = TimeSeriesModel().apply {
        addTimeSeries(title).series.apply { points.forEach { (x, y) -> add(x, y) } }
        isAutoRange = false
        rangeLowerBound = 0.0
        rangeUpperBound = 1.0
    }.let { model -> TimeSeriesPlotPanel(model).apply {
        preferredSize = Dimension(390, 300)
        seriesRemovalEnabled = false
        chartPanel.chart.xyPlot.domainAxis.label = xAxis
        chartPanel.chart.xyPlot.rangeAxis.label = yAxis
    } }

    val wholeList = plot(
        "Whole-list accuracy", "List length", "Proportion recalled perfectly",
        evaluation.wholeListAccuracy.mapIndexed { index, value -> index + 1 to value }
    )
    val serialPosition = plot(
        "Serial-position accuracy", "Position in target list", "Proportion correct",
        evaluation.serialPositionAccuracy.mapNotNull { (index, value) -> value?.let { index + 1 to it } }
    )
    val transpositions = plot(
        "Transposition proportion", "Source position − recalled position", "Proportion of errors",
        evaluation.transpositionDistance.mapIndexed { index, value -> index - maxListLength to value }
    )
    JFrame("Botvinick-Plaut Serial Recall Evaluation").apply {
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        layout = BorderLayout(12, 12)
        add(JPanel(GridLayout(1, 3, 12, 0)).apply {
            border = BorderFactory.createEmptyBorder(8, 12, 12, 12)
            add(wholeList)
            add(serialPosition)
            add(transpositions)
        }, BorderLayout.CENTER)
        pack()
        setLocationByPlatform(true)
        isVisible = true
    }
}

private fun DoubleArray.dot(other: DoubleArray) = indices.sumOf { this[it] * other[it] }

private fun sigmoid(value: Double) = 1.0 / (1.0 + exp(-value))

private fun DoubleArray.softmax(): DoubleArray {
    val maximum = maxOrNull() ?: 0.0
    val exponentials = DoubleArray(size) { exp(this[it] - maximum) }
    val total = exponentials.sum()
    return DoubleArray(size) { exponentials[it] / total }
}

private fun DoubleArray.argmax(limit: Int) = (0 until limit).maxBy { this[it] }

private fun Array<DoubleArray>.applyGradient(gradient: Array<DoubleArray>, scale: Double) = indices.forEach { row ->
    this[row].indices.forEach { column -> this[row][column] -= scale * gradient[row][column] }
}

private fun DoubleArray.normalize(): DoubleArray {
    val total = sum()
    return if (total == 0.0) copyOf() else DoubleArray(size) { this[it] / total }
}

private fun smile.math.matrix.Matrix.copyValuesFrom(values: Array<DoubleArray>) {
    values.indices.forEach { row -> values[row].indices.forEach { column -> this[row, column] = values[row][column] } }
}
