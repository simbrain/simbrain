package org.simbrain.custom_sims.simulations.psychology

import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addSynapse
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.util.format
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showMessageDialog
import kotlin.math.*
import kotlin.random.Random

val serialOrderMemory = newSim {

    workspace.clearWorkspace()

    val itemLabels = listOf("A", "B", "C", "D", "E", "F")
    val maxListLength = 6
    val recallCueLabel = "Recall Cue"
    val endLabel = "End"
    val random = Random(42)

    var condition = SerialOrderCondition.Arbitrary
    var listLength = 5
    var noiseLevel = 0.18
    var transitionBias = 0.35
    var phase = SerialOrderPhase.Ready
    var encodeIndex = 0
    var recallIndex = 0
    var targetList = emptyList<Int>()
    val recalledList = mutableListOf<Int>()
    val memory = Array(maxListLength) { DoubleArray(itemLabels.size) }

    val networkComponent = addNetworkComponent("Serial Order Memory Toy Model")
    val network = networkComponent.network
    val inputY = 520.0
    val hiddenStartX = 430.0
    val hiddenStartY = 40.0
    val outputStartX = 900.0
    val outputY = 520.0
    val neuronSpacing = 58.0

    fun labeledNeuron(label: String, x: Double, y: Double) = Neuron().apply {
        this.label = label
        location = point(x, y)
        clamped = true
    }

    val inputNeurons = (itemLabels + recallCueLabel).mapIndexed { index, label ->
        labeledNeuron(label, index * neuronSpacing, inputY)
    }
    val hiddenNeurons = (0 until maxListLength).flatMap { position ->
        itemLabels.mapIndexed { item, label ->
            labeledNeuron("${position + 1}:$label", hiddenStartX + item * neuronSpacing, hiddenStartY + position * neuronSpacing)
        }
    }
    val outputNeurons = (itemLabels + endLabel).mapIndexed { index, label ->
        labeledNeuron(label, outputStartX + index * neuronSpacing, outputY)
    }

    network.addNetworkModels(inputNeurons + hiddenNeurons + outputNeurons)

    val inputCollection = NeuronCollection(inputNeurons).apply {
        label = "Input"
        layout = LineLayout(neuronSpacing, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout(point(0.0, inputY))
    }
    val hiddenCollection = NeuronCollection(hiddenNeurons).apply {
        label = "Recurrent Memory"
        layout = GridLayout(neuronSpacing, neuronSpacing, itemLabels.size)
        applyLayout(point(hiddenStartX, hiddenStartY))
    }
    val outputCollection = NeuronCollection(outputNeurons).apply {
        label = "Recall Output"
        layout = LineLayout(neuronSpacing, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout(point(outputStartX, outputY))
    }
    network.addNetworkModels(inputCollection, hiddenCollection, outputCollection)

    for (position in 0 until maxListLength) {
        for (item in itemLabels.indices) {
            val hidden = hiddenNeurons[position * itemLabels.size + item]
            network.addSynapse(inputNeurons[item], hidden) { strength = 1.0 }
            network.addSynapse(hidden, hidden) { strength = 0.92 }
            network.addSynapse(hidden, outputNeurons[item]) { strength = 1.0 }
        }
    }
    network.addSynapse(inputNeurons.last(), outputNeurons.last()) { strength = 0.4 }

    fun clearActivations() {
        inputNeurons.forEach { it.activation = 0.0 }
        hiddenNeurons.forEach { it.activation = 0.0 }
        outputNeurons.forEach { it.activation = 0.0 }
    }

    fun clearMemory() {
        memory.forEach { row -> row.fill(0.0) }
    }

    fun updateHiddenDisplay() {
        for (position in 0 until maxListLength) {
            for (item in itemLabels.indices) {
                hiddenNeurons[position * itemLabels.size + item].activation = memory[position][item]
            }
        }
    }

    fun updateInputDisplay(item: Int?, recallCue: Boolean = false) {
        inputNeurons.forEach { it.activation = 0.0 }
        if (item != null) {
            inputNeurons[item].activation = 1.0
        }
        if (recallCue) {
            inputNeurons.last().activation = 1.0
        }
    }

    fun updateOutputDisplay(item: Int?, end: Boolean = false) {
        outputNeurons.forEach { it.activation = 0.0 }
        if (item != null) {
            outputNeurons[item].activation = 1.0
        }
        if (end) {
            outputNeurons.last().activation = 1.0
        }
    }

    fun renderState() {
        updateHiddenDisplay()
        when (phase) {
            SerialOrderPhase.Encoding -> updateInputDisplay(targetList.getOrNull(encodeIndex))
            SerialOrderPhase.Recall -> updateInputDisplay(null, recallCue = true)
            else -> updateInputDisplay(null)
        }
    }

    fun itemName(index: Int) = itemLabels[index]

    fun sequenceText(sequence: List<Int>) = if (sequence.isEmpty()) "(none)" else sequence.joinToString(" ") { itemName(it) }

    fun newTargetList(): List<Int> {
        return when (condition) {
            SerialOrderCondition.Arbitrary, SerialOrderCondition.Confusable -> itemLabels.indices.shuffled(random).take(listLength)
            SerialOrderCondition.Structured -> {
                val aGroup = listOf(0, 2, 4).shuffled(random)
                val bGroup = listOf(1, 3, 5).shuffled(random)
                buildList {
                    for (i in 0 until listLength) {
                        val group = if (i % 2 == 0) aGroup else bGroup
                        add(group[i / 2])
                    }
                }
            }
        }
    }

    fun resetTrial(nextTargetList: List<Int> = newTargetList()) {
        clearMemory()
        clearActivations()
        targetList = nextTargetList
        recalledList.clear()
        encodeIndex = 0
        recallIndex = 0
        phase = SerialOrderPhase.Encoding
        renderState()
    }

    fun encodeStep() {
        if (phase != SerialOrderPhase.Encoding) {
            return
        }
        val item = targetList.getOrNull(encodeIndex) ?: return
        updateInputDisplay(item)
        memory[encodeIndex].fill(0.0)
        memory[encodeIndex][item] = 1.0
        encodeIndex += 1
        if (encodeIndex >= targetList.size) {
            phase = SerialOrderPhase.Recall
            recallIndex = 0
            updateInputDisplay(null, recallCue = true)
        }
        updateHiddenDisplay()
    }

    fun hiddenActivation(position: Int, item: Int): Double {
        return hiddenNeurons[position * itemLabels.size + item].activation
    }

    fun gaussianNoise(scale: Double): Double {
        if (scale <= 0.0) return 0.0
        val u1 = random.nextDouble().coerceAtLeast(1.0e-12)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2) * scale
    }

    fun similarItem(candidate: Int, encoded: Int): Double {
        if (candidate == encoded) return 1.0
        return if (condition == SerialOrderCondition.Confusable && candidate / 2 == encoded / 2) 0.58 else 0.0
    }

    fun transitionScore(candidate: Int): Double {
        if (condition != SerialOrderCondition.Structured || recalledList.isEmpty()) return 0.0
        val previous = recalledList.last()
        val alternatesGroup = previous % 2 != candidate % 2
        return if (alternatesGroup) transitionBias else -transitionBias * 0.35
    }

    fun recallCandidate(): Int {
        val scores = itemLabels.indices.map { candidate ->
            var score = transitionScore(candidate)
            for (position in 0 until targetList.size) {
                val distance = kotlin.math.abs(position - recallIndex)
                val positionWeight = exp(-distance.toDouble() * 1.55)
                for (encoded in itemLabels.indices) {
                    score += positionWeight * hiddenActivation(position, encoded) * similarItem(candidate, encoded)
                }
            }
            if (candidate in recalledList) {
                score -= 0.75
            }
            candidate to score + gaussianNoise(noiseLevel + (targetList.size - 1) * 0.025)
        }
        return scores.maxBy { it.second }.first
    }

    fun recallStep(): Int? {
        if (phase != SerialOrderPhase.Recall) return null
        updateInputDisplay(null, recallCue = true)
        val item = recallCandidate()
        recalledList.add(item)
        recallIndex += 1
        updateOutputDisplay(item)
        if (recallIndex >= targetList.size) {
            phase = SerialOrderPhase.Done
            updateInputDisplay(null, recallCue = true)
            outputNeurons.last().activation = 1.0
        }
        return item
    }

    fun stepTrial() {
        when (phase) {
            SerialOrderPhase.Encoding -> encodeStep()
            SerialOrderPhase.Recall -> recallStep()
            SerialOrderPhase.Ready, SerialOrderPhase.Done -> return
        }
    }

    fun runTrial(nextTargetList: List<Int>? = null) {
        if (nextTargetList == null) {
            resetTrial()
        } else {
            resetTrial(nextTargetList)
        }
        while (phase == SerialOrderPhase.Encoding) {
            encodeStep()
        }
        while (phase == SerialOrderPhase.Recall) {
            recallStep()
        }
    }

    fun trialAccuracy(target: List<Int>, response: List<Int>): Double {
        if (target.isEmpty()) return 0.0
        return target.zip(response).count { (a, b) -> a == b }.toDouble() / target.size
    }

    fun benchmarkLine(label: String, trials: Int, setup: () -> Unit): String {
        setup()
        var total = 0.0
        repeat(trials) {
            runTrial()
            total += trialAccuracy(targetList, recalledList)
        }
        return "$label: ${(100.0 * total / trials).format(1)}%"
    }

    fun benchmarkLine(label: String, trials: Int, setup: () -> Unit, targetFactory: () -> List<Int>): String {
        setup()
        var total = 0.0
        repeat(trials) {
            runTrial(targetFactory())
            total += trialAccuracy(targetList, recalledList)
        }
        return "$label: ${(100.0 * total / trials).format(1)}%"
    }

    fun runBenchmark(): String {
        val savedCondition = condition
        val savedLength = listLength
        val savedNoise = noiseLevel
        val savedBias = transitionBias
        val trials = 120
        val result = buildString {
            appendLine("Mean item-in-position accuracy over $trials trials")
            appendLine()
            appendLine(benchmarkLine("Length 3", trials) {
                condition = SerialOrderCondition.Arbitrary
                listLength = 3
                noiseLevel = savedNoise
                transitionBias = savedBias
            })
            appendLine(benchmarkLine("Length 6", trials) {
                condition = SerialOrderCondition.Arbitrary
                listLength = 6
            })
            appendLine(benchmarkLine("Confusable length 6", trials) {
                condition = SerialOrderCondition.Confusable
                listLength = 6
            })
            appendLine(benchmarkLine("Structured high-probability", trials) {
                condition = SerialOrderCondition.Structured
                listLength = 6
                transitionBias = savedBias.coerceAtLeast(0.25)
            })
            appendLine(benchmarkLine("Structured low-probability", trials, setup = {
                condition = SerialOrderCondition.Structured
                listLength = 6
                transitionBias = savedBias.coerceAtLeast(0.25)
            }, targetFactory = {
                listOf(0, 2, 4, 1, 3, 5)
            }))
        }
        condition = savedCondition
        listLength = savedLength
        noiseLevel = savedNoise
        transitionBias = savedBias
        resetTrial()
        return result
    }

    withGui {
        val controlPanel = createControlPanel("Serial Recall Toy", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            val targetText = addLabelledText("Target", sequenceText(targetList))
            val recalledText = addLabelledText("Recalled", sequenceText(recalledList))
            val phaseText = addLabelledText("Phase", phase.displayName)
            val accuracyText = addLabelledText("Accuracy", "0.0%")

            fun refreshLabels() {
                targetText.text = sequenceText(targetList)
                recalledText.text = sequenceText(recalledList)
                phaseText.text = phase.displayName
                accuracyText.text = "${(100.0 * trialAccuracy(targetList, recalledList)).format(1)}%"
            }

            resetTrial()
            refreshLabels()

            addComboBox("Condition", SerialOrderCondition.entries.toList(), condition) {
                condition = it
                resetTrial()
                refreshLabels()
            }
            addComboBox("List length", (3..maxListLength).toList(), listLength) {
                listLength = it
                resetTrial()
                refreshLabels()
            }
            addSlider("Noise", 0.0, 0.6, noiseLevel, 0.05) {
                noiseLevel = it
            }
            addSlider("Transition bias", 0.0, 0.8, transitionBias, 0.05) {
                transitionBias = it
            }
            addButton("Reset") {
                resetTrial()
                refreshLabels()
            }
            addButton("Step") {
                stepTrial()
                refreshLabels()
            }
            addSeparator()
            addButton("Run Trial") {
                runTrial()
                refreshLabels()
            }
            addButton("Run Benchmark") {
                showMessageDialog(runBenchmark(), "Serial Order Benchmark")
                refreshLabels()
            }
        }.awaitLayout()

        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 1050, 660)
    }

    addSidebarInfo(
        """
        # Serial Order Memory Toy Model

        This toy simulation illustrates the main idea in Botvinick and Plaut's recurrent neural network account of short-term memory for serial order. A list is encoded as a sustained pattern of activation in a recurrent memory layer, and recall reads out that activation after a `Recall Cue`.

        # Simulation Details

        The simulation is a compact teaching model, not a reproduction of the paper's full trained network. There is no BPTT, learned recurrent dynamics, or weight-training phase in this version. During a trial, `Step` first loads temporary activation traces into the recurrent memory layer and then reads those traces using fixed readout weights. The point is to illustrate activation-based memory for order, not learning by changing weights during a trial.

        ## Control Panel Settings

        `Condition` changes the source of recall errors. `Arbitrary` uses distinct items, `Confusable` makes item pairs overlap, and `Structured` biases recall toward alternating item groups to illustrate how background sequence knowledge can regularize recall. `Noise` controls degradation of the hidden trace. `Transition bias` controls how strongly the structured condition favors familiar transitions.

        ## Control Panel Buttons

        `Reset` clears visible activations, clears the recurrent memory trace, and draws a new target list.

        `Step` advances the current trial by one state-appropriate step. During `Encoding`, it presents one target item and writes its item-position trace into the recurrent memory layer. After the last item is encoded, the phase label switches to `Recall` and the `Recall Cue` activates. During `Recall`, `Step` reads the visible recurrent memory activations, applies noise, confusability, response suppression, and any structured transition bias, then activates one recall-output neuron. When the phase is `Done`, `Step` has no effect; use `Reset` for a new list.

        This is not weight training. The demo has fixed readout weights; encoding loads a temporary activation trace, and recall reads that trace. If you randomize or edit the recurrent memory neurons before recall, the recall response changes because the readout uses those visible activations.

        `Run Trial` performs a full fresh trial: reset, encode the full list, and recall the full list.

        `Run Benchmark` runs repeated trials and reports qualitative accuracy patterns for shorter versus longer lists, confusable lists, and structured high- versus low-probability lists.

        # What to Do

        1. Click `Reset`, then use `Step` to watch items enter the recurrent memory layer.
        2. Continue pressing `Step` after the phase label switches to `Recall` to recall the list from the sustained hidden activation.
        3. Increase `Noise` or `List length` and observe more transposition and item errors.
        4. Compare `Arbitrary`, `Confusable`, and `Structured` with `Run Benchmark`.

        Use the buttons in `Serial Recall Toy` to run the simulation. The main Simbrain `Run` button is not needed for this demo.

        # References

        Botvinick, M. M., & Plaut, D. C. (2006). [_Short-term memory for serial order: A recurrent neural network model_](https://doi.org/10.1037/0033-295X.113.2.201). _Psychological Review_, _113_(2), 201-233.
        """.trimIndent(),
        width = 360
    )
}

private enum class SerialOrderCondition {
    Arbitrary,
    Confusable,
    Structured
}

private enum class SerialOrderPhase(val displayName: String) {
    Ready("Ready"),
    Encoding("Encoding"),
    Recall("Recall"),
    Done("Done")
}
