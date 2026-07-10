package org.simbrain.custom_sims.simulations.psychology

import org.simbrain.custom_sims.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addSynapse
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.util.format
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showMessageDialog
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

val serialOrderContextMemory = newSim {

    workspace.clearWorkspace()

    val itemLabels = listOf("A", "B", "C", "D", "E", "F")
    val maxListLength = itemLabels.size
    val contextWindowSize = 6
    val contextNodeCount = contextWindowSize + maxListLength - 1
    val random = Random(73)

    var listLength = 5
    var outputNoise = 0.02
    var weightDecay = 0.98
    var phase = PositionalContextPhase.Ready
    var encodeIndex = 0
    var recallIndex = 0
    var targetList = emptyList<Int>()
    val recalledList = mutableListOf<Int>()

    val networkComponent = addNetworkComponent("Burgess-Hitch Positional Context Network")
    val network = networkComponent.network

    fun clampedNeuron(label: String) = Neuron().apply {
        this.label = label
        clamped = true
    }

    val contextNeurons = (1..contextNodeCount).map { clampedNeuron("T$it") }
    val itemNeurons = itemLabels.map { clampedNeuron(it) }
    val suppressionNeurons = itemLabels.map { clampedNeuron("−$it") }
    network.addNetworkModels(contextNeurons + itemNeurons + suppressionNeurons)

    val contextLayer = NeuronCollection(contextNeurons).apply {
        label = "Positional context"
        layout = LineLayout(62.0, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout(point(80.0, 480.0))
    }
    val itemLayer = NeuronCollection(itemNeurons).apply {
        label = "Letter competition"
        layout = LineLayout(120.0, LineLayout.LineOrientation.HORIZONTAL)
        applyLayout(point(235.0, 120.0))
    }
    network.addNetworkModels(contextLayer, itemLayer)
    alignNetworkModels(contextLayer, itemLayer, Alignment.VERTICAL)
    suppressionNeurons.forEachIndexed { index, neuron ->
        neuron.location = point(itemNeurons[index].location.x + 35.0, itemNeurons[index].location.y + 100.0)
    }
    network.addNetworkModels(NeuronCollection(suppressionNeurons).apply { label = "Suppression traces" })

    val contextToItem = Array(contextNodeCount) { context ->
        Array(itemLabels.size) { item ->
            network.addSynapse(contextNeurons[context], itemNeurons[item]) { strength = 0.0 }
        }
    }
    val suppressionSynapses = itemLabels.indices.map { item ->
        network.addSynapse(suppressionNeurons[item], itemNeurons[item]) { strength = -1.0 }
    }

    fun itemName(index: Int) = itemLabels[index]

    fun sequenceText(sequence: List<Int>) =
        if (sequence.isEmpty()) "(none)" else sequence.joinToString(" ") { itemName(it) }

    fun activeContextIndices(position: Int) = position until position + contextWindowSize

    fun setContext(position: Int) {
        contextNeurons.forEach { it.activation = 0.0 }
        activeContextIndices(position).forEach { contextNeurons[it].activation = 1.0 }
    }

    fun setWinningItem(item: Int?) {
        itemNeurons.forEach { it.activation = 0.0 }
        item?.let { itemNeurons[it].activation = 1.0 }
    }

    fun clearWeights() {
        contextToItem.flatten().forEach { it.strength = 0.0 }
    }

    fun decayMemory() {
        contextToItem.flatten().forEach { it.strength *= weightDecay }
        suppressionNeurons.forEach { it.activation *= weightDecay }
    }

    fun hebbianEncode(item: Int) {
        val maximumWeight = 1.0 / contextWindowSize
        activeContextIndices(encodeIndex).forEach { context ->
            contextToItem[context][item].strength = maximumWeight
        }
    }

    fun gaussianNoise(): Double {
        if (outputNoise == 0.0) return 0.0
        val u1 = random.nextDouble().coerceAtLeast(1.0e-12)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2) * outputNoise
    }

    fun synapticInput(item: Int): Double = contextToItem.indices.sumOf { context ->
        contextToItem[context][item].strength * contextNeurons[context].activation
    }

    fun selectWinner(withNoise: Boolean): Int = itemLabels.indices.maxBy { item ->
        synapticInput(item) + suppressionSynapses[item].strength * suppressionNeurons[item].activation +
            if (withNoise) gaussianNoise() else 0.0
    }

    fun newTargetList() = itemLabels.indices.shuffled(random).take(listLength)

    fun resetTrial(nextTargetList: List<Int> = newTargetList()) {
        clearWeights()
        contextNeurons.forEach { it.activation = 0.0 }
        setWinningItem(null)
        suppressionNeurons.forEach { it.activation = 0.0 }
        targetList = nextTargetList
        recalledList.clear()
        encodeIndex = 0
        recallIndex = 0
        phase = PositionalContextPhase.Encoding
    }

    fun encodeStep() {
        if (phase != PositionalContextPhase.Encoding) return
        val presentedItem = targetList.getOrNull(encodeIndex) ?: return
        setContext(encodeIndex)
        setWinningItem(presentedItem)
        hebbianEncode(presentedItem)
        decayMemory()
        encodeIndex++
        if (encodeIndex == targetList.size) {
            setWinningItem(null)
            phase = PositionalContextPhase.Recall
        }
    }

    fun recallStep() {
        if (phase != PositionalContextPhase.Recall) return
        setContext(recallIndex)
        val winner = selectWinner(withNoise = true)
        setWinningItem(winner)
        recalledList.add(winner)
        suppressionNeurons[winner].activation = 2.0
        decayMemory()
        recallIndex++
        if (recallIndex == targetList.size) phase = PositionalContextPhase.Done
    }

    fun stepTrial() = when (phase) {
        PositionalContextPhase.Encoding -> encodeStep()
        PositionalContextPhase.Recall -> recallStep()
        PositionalContextPhase.Ready, PositionalContextPhase.Done -> Unit
    }

    fun runTrial() {
        resetTrial()
        while (phase == PositionalContextPhase.Encoding) encodeStep()
        while (phase == PositionalContextPhase.Recall) recallStep()
    }

    fun trialAccuracy() = if (targetList.isEmpty()) 0.0 else
        targetList.zip(recalledList).count { (target, response) -> target == response }.toDouble() / targetList.size

    fun transpositionDistances(): List<Int> {
        val targetPositions = targetList.withIndex().associate { it.value to it.index }
        return recalledList.withIndex().mapNotNull { (position, item) ->
            val targetPosition = targetPositions[item]
            if (targetPosition == null || targetPosition == position) null else {
                kotlin.math.abs(position - targetPosition)
            }
        }
    }

    fun benchmarkCondition(label: String, trials: Int, setup: () -> Unit): String {
        setup()
        var accuracy = 0.0
        var errorDistance = 0.0
        var transpositionCount = 0
        var intrusionCount = 0
        repeat(trials) {
            runTrial()
            accuracy += trialAccuracy()
            val distances = transpositionDistances()
            errorDistance += distances.sum()
            transpositionCount += distances.size
            intrusionCount += recalledList.count { it !in targetList }
        }
        val distance = if (transpositionCount == 0) "no in-list transpositions" else
            "mean transposition distance ${(errorDistance / transpositionCount).format(2)}"
        val intrusionRate = 100.0 * intrusionCount / (trials * listLength)
        return "$label: ${(100.0 * accuracy / trials).format(1)}% accuracy, $distance, " +
            "${intrusionRate.format(1)}% intrusions"
    }

    fun runBenchmark(): String {
        val trials = 200
        val savedNoise = outputNoise
        val savedDecayMultiplier = weightDecay
        return try {
            buildString {
                appendLine("Burgess-Hitch positional-context core over $trials trials per condition")
                appendLine("List length $listLength")
                appendLine()
                appendLine("Letter-selection noise sweep (retention per step ${savedDecayMultiplier.format(2)})")
                listOf(0.00, 0.04, 0.08, 0.16).forEach { noise ->
                    appendLine(benchmarkCondition("Noise ${noise.format(2)}", trials) { outputNoise = noise })
                }
                appendLine()
                appendLine("Retention-per-step sweep (letter-selection noise ${savedNoise.format(2)})")
                listOf(1.00, 0.98, 0.94, 0.86).forEach { multiplier ->
                    appendLine(benchmarkCondition("Multiplier ${multiplier.format(2)}", trials) { weightDecay = multiplier })
                }
            }
        } finally {
            outputNoise = savedNoise
            weightDecay = savedDecayMultiplier
            resetTrial()
        }
    }

    withGui {
        val controlPanel = createControlPanel("Positional Context Recall", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            val targetText = addLabelledText("Target", sequenceText(targetList))
            val recalledText = addLabelledText("Recalled", sequenceText(recalledList))
            val phaseText = addLabelledText("Phase", phase.displayName)
            val positionText = addLabelledText("Position", "-")
            val accuracyText = addLabelledText("Accuracy", "0.0%")

            fun refreshLabels() {
                targetText.text = sequenceText(targetList)
                recalledText.text = sequenceText(recalledList)
                phaseText.text = phase.displayName
                val position = when (phase) {
                    PositionalContextPhase.Encoding -> encodeIndex + 1
                    PositionalContextPhase.Recall -> recallIndex + 1
                    PositionalContextPhase.Done -> recallIndex
                    PositionalContextPhase.Ready -> 0
                }
                positionText.text = if (position == 0) "-" else position.toString()
                accuracyText.text = "${(100.0 * trialAccuracy()).format(1)}%"
            }

            resetTrial()
            refreshLabels()
            addComboBox("List length", (3..maxListLength).toList(), listLength) {
                listLength = it
                resetTrial()
                refreshLabels()
            }
            addSlider(
                "Letter-selection noise", 0.0, 0.6, outputNoise, 0.02,
                toolTip = "Gaussian noise added independently to each letter's recall input immediately before selection. Positional context is unchanged."
            ) { outputNoise = it }
            addSlider(
                "Retention per step", 0.75, 1.0, weightDecay, 0.01,
                toolTip = "Fraction of temporary binding weights and suppression traces retained after each step. 1.00 retains 100%; lower values cause faster decay."
            ) { weightDecay = it }
            addButton("Reset") { resetTrial(); refreshLabels() }
            addButton("Step") { stepTrial(); refreshLabels() }
            addSeparator()
            addButton("Run Trial") { runTrial(); refreshLabels() }
            addButton("Run Benchmark") {
                showMessageDialog(runBenchmark(), "Positional Context Benchmark")
                refreshLabels()
            }
        }.awaitLayout()

        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 1050, 650)
    }

    addSidebarInfo(
        """
        # Burgess-Hitch Positional Context Memory

        This simulation implements the positional-context core of Burgess and Hitch's 1999 neural-network model of immediate serial recall. It uses the paper's context and letter nodes, plastic context-to-letter connections, winner-take-all letter selection, and decaying inhibition of selected letters. It deliberately omits the model's input and output phoneme layers so the serial-order mechanism remains visible.

        # Background

        `Positional context` is the paper's moving window. Six adjacent binary timing nodes are active at each position, and the window shifts one node for the next letter. Successive context states therefore share five of six active nodes, while more distant states overlap less.

        During encoding, the externally presented letter wins in the localist `Letter competition` layer. Hebbian learning strengthens the actual Simbrain synapses from every active context node to that letter. Each weight is capped at `1 / 6`, as in the paper's normalization by the number of active context nodes. The short-term weights are retained by a fixed fraction after every step.

        During recall, the context window is reset and replayed exactly as it was during encoding. Each letter receives the weighted sum arriving through its visible context synapses, plus its current inhibition and Gaussian letter-selection noise. The strongest letter wins, all other letter activations become zero, and the winner's matching `−letter` suppression trace is set to 2. Its fixed -1 synapse supplies inhibition of -2; the trace then decays on later steps, implementing competitive queuing and discouraging immediate repetitions.

        The noise is not a visible context or letter activation. At each recall position, an independent Gaussian value is added internally to every letter's input immediately before winner-take-all selection. You see it only when it changes which letter wins. The context window itself is replayed without noise.

        The paper specifies functional network layers, not a one-to-one anatomical map. The positional-context signal is a candidate for a distributed timing or temporal-context representation in working-memory circuitry; letter nodes stand for lexical representations in language-related cortex. The simulation should therefore be read as a neural process model, not as a diagram of particular brain regions.

        # What To Do

        Press `Step` through encoding. Watch the context window move and the synapses to the currently presented letter strengthen. During recall there is no external letter input: the replayed context drives letter competition through those learned synapses.

        Start with the default settings and press `Run Trial`: recall should be nearly perfect. Then experiment with one parameter at a time:

        - Lower `Retention per step`. Each step then preserves less of the temporary context-to-letter weights, especially weakening early letters before recall, so accuracy falls. A value of 1.00 retains 100%; lower values mean more decay.
        - Raise `Letter-selection noise`. The context sequence remains unchanged, but the hidden noise added to letter competition can make a competing letter win. Because neighboring contexts overlap most, errors are usually nearby transpositions.

        `Run Benchmark` runs two four-condition sweeps, with 200 trials per condition, at the current list length. The noise sweep holds the selected retention-per-step value fixed; the retention-per-step sweep holds the selected letter-selection noise fixed. It reports accuracy, transposition distance, and intrusions, then restores both controls.

        Transposition distance is the number of positions between a wrongly recalled letter's original and recalled locations. For example, if `A B C D` is recalled as `A C B D`, both `B` and `C` have distance 1. The benchmark reports the mean distance for in-list transpositions; intrusions are recalled letters not present in the target list.

        This is a stripped-down implementation, not the complete phonological-loop model. The omitted phoneme layers are needed for phonological similarity, word length, modality, rehearsal, and lexicality effects; the implemented context-letter competitive-queuing circuit is the mechanism that stores and recalls serial order.

        # Reference

        Burgess, N., & Hitch, G. J. (1999). [_Memory for serial order: A network model of the phonological loop and its timing_](https://doi.org/10.1037/0033-295X.106.3.551). _Psychological Review_, _106_(3), 551-581.
        """.trimIndent(),
        width = 440
    )
}

private enum class PositionalContextPhase(val displayName: String) {
    Ready("Ready"),
    Encoding("Encoding"),
    Recall("Recall"),
    Done("Done")
}
