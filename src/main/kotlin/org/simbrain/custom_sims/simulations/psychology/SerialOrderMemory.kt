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

    var listLength = 5
    var noiseLevel = 0.18
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
        return itemLabels.indices.shuffled(random).take(listLength)
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

    fun setHiddenActivation(position: Int, item: Int, value: Double) {
        hiddenNeurons[position * itemLabels.size + item].activation = value
    }

    fun gaussianNoise(scale: Double): Double {
        if (scale <= 0.0) return 0.0
        val u1 = random.nextDouble().coerceAtLeast(1.0e-12)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2) * scale
    }

    fun applyVisibleNoiseToHiddenTrace() {
        for (position in 0 until maxListLength) {
            for (item in itemLabels.indices) {
                val baseActivation = if (position < targetList.size) memory[position][item] else 0.0
                setHiddenActivation(position, item, (baseActivation + gaussianNoise(noiseLevel)).coerceIn(0.0, 1.0))
            }
        }
    }

    fun showRecallConfusion(recalledItem: Int) {
        val targetItem = targetList.getOrNull(recallIndex) ?: return
        if (recalledItem == targetItem) return
        for (item in itemLabels.indices) {
            setHiddenActivation(recallIndex, item, hiddenActivation(recallIndex, item) * 0.25)
        }
        setHiddenActivation(recallIndex, targetItem, max(hiddenActivation(recallIndex, targetItem), 0.25))
        setHiddenActivation(recallIndex, recalledItem, 1.0)
    }

    fun recallCandidate(): Int {
        val scores = itemLabels.indices.map { candidate ->
            var score = 0.0
            for (position in 0 until targetList.size) {
                val distance = kotlin.math.abs(position - recallIndex)
                val positionWeight = exp(-distance.toDouble() * 1.55)
                score += positionWeight * hiddenActivation(position, candidate)
            }
            if (candidate in recalledList) {
                score -= 0.75
            }
            candidate to score
        }
        return scores.maxBy { it.second }.first
    }

    fun recallStep(): Int? {
        if (phase != SerialOrderPhase.Recall) return null
        updateInputDisplay(null, recallCue = true)
        applyVisibleNoiseToHiddenTrace()
        val item = recallCandidate()
        showRecallConfusion(item)
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

    fun runBenchmark(): String {
        val savedLength = listLength
        val savedNoise = noiseLevel
        val trials = 120
        val result = buildString {
            appendLine("Mean item-in-position accuracy over $trials trials")
            appendLine()
            appendLine(benchmarkLine("Length 3, low noise", trials) {
                listLength = 3
                noiseLevel = 0.05
            })
            appendLine(benchmarkLine("Length 6, low noise", trials) {
                listLength = 6
                noiseLevel = 0.05
            })
            appendLine(benchmarkLine("Length 6, high noise", trials) {
                listLength = 6
                noiseLevel = 0.45
            })
        }
        listLength = savedLength
        noiseLevel = savedNoise
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

            addComboBox("List length", (3..maxListLength).toList(), listLength) {
                listLength = it
                resetTrial()
                refreshLabels()
            }
            addSlider("Noise", 0.0, 0.6, noiseLevel, 0.05) {
                noiseLevel = it
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

        # Background

        Botvinick and Plaut's model is close to a simple recurrent network in spirit: a recurrent hidden state carries information from one time step to the next, and the network is trained over sequences. The key theoretical point is that the remembered list is stored in sustained activation during a trial, not in new temporary weights.

        This contrasts with many context-based serial-recall models. In those models, item representations are temporarily bound to an independent position or temporal-context representation. In connectionist versions, those item-context bindings are often implemented as short-term weight changes. This is different from Elman-style `context units`, which are just previous hidden activations and are therefore activation-based.

        This toy model is not a full reproduction of the paper's trained recurrent network. There is no BPTT, learned recurrent dynamics, or weight-training phase. It is a compact visual demonstration of the activation-based idea: `Step` first loads temporary activation traces into the recurrent memory layer and then reads those traces using fixed readout weights.

        The toy focuses on one qualitative effect discussed in the paper: recall becomes worse when more items must be maintained, especially when the activation trace is noisy. It is meant to make that activation-based memory idea visible, not to reproduce the paper's quantitative simulations.

        # Simulation Details

        ## Control Panel

        `List length` changes how many item-position traces must be maintained. Longer lists are harder because recall has to read from a larger activation pattern.

        `Noise` controls random variability in the recurrent memory grid during recall. Higher noise visibly jitters the stored item-position traces and makes the readout more likely to choose a nearby or competing item instead of the correct one.

        `Reset` clears visible activations, clears the recurrent memory trace, and draws a new target list.

        `Step` advances the current trial by one state-appropriate step. During `Encoding`, it presents one target item and writes its item-position trace into the recurrent memory layer. After the last item is encoded, the phase label switches to `Recall` and the `Recall Cue` activates. During `Recall`, `Step` visibly jitters the recurrent memory trace, reads the visible recurrent memory activations, applies response suppression, then activates one recall-output neuron. When the phase is `Done`, `Step` has no effect; use `Reset` for a new list.

        This is not weight training. The demo has fixed readout weights; encoding loads a temporary activation trace, and recall reads that trace. If you randomize or edit the recurrent memory neurons before recall, the recall response changes because the readout uses those visible activations.

        `Run Trial` performs a full fresh trial: reset, encode the full list, and recall the full list.

        `Run Benchmark` runs repeated trials and reports qualitative accuracy patterns for shorter versus longer lists and low versus high noise.

        ## Under the Hood

        This toy uses a small rule system. The visible network is a diagram and state display; ordinary Simbrain network stepping is not doing the computation.

        During encoding, `Step` writes a one-hot trace into the recurrent memory grid. For example, if the first item is `C`, the neuron `1:C` is activated. If the second item is `A`, `2:A` is activated. This is intentionally localist: there is one visible memory neuron for each possible item-position pair.

        The full Botvinick and Plaut model is different. Its hidden layer is distributed, so item and position information are encoded as patterns across many hidden units, not as one explicit neuron for each item-position combination. The toy uses the localist grid because it makes the activation trace easy to see.

        During recall, the code first renders a noisy version of the stored trace into the recurrent memory grid. Then it scores each possible output item. The score is based mostly on the activation in the current memory row, with weaker contributions from nearby rows and a penalty for already recalled items. The highest-scoring item is selected. If the selected item is wrong, the current memory row is briefly shifted toward that recalled item so the visible trace reflects the confusion.

        The visible synapses are schematic. They show the intended flow from input to recurrent memory to recall output, but the toy model's recall behavior is controlled by this rule-based scorer rather than by learned recurrent weights.

        # What to Do

        1. Click `Reset`, then use `Step` to watch items enter the recurrent memory layer.
        2. Continue pressing `Step` after the phase label switches to `Recall` to recall the list from the sustained hidden activation.
        3. Increase `Noise` or `List length` and observe more recall errors.
        4. Use `Run Benchmark` to compare short, long, and noisy-list recall.

        Use the buttons in `Serial Recall Toy` to run the simulation. The main Simbrain `Run` button is not needed for this demo.

        # References

        Botvinick, M. M., & Plaut, D. C. (2006). [_Short-term memory for serial order: A recurrent neural network model_](https://doi.org/10.1037/0033-295X.113.2.201). _Psychological Review_, _113_(2), 201-233.
        """.trimIndent(),
        width = 360
    )
}

private enum class SerialOrderPhase(val displayName: String) {
    Ready("Ready"),
    Encoding("Encoding"),
    Recall("Recall"),
    Done("Done")
}
