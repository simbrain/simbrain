/**
 * An interactive, component-level demonstration of modern Hopfield retrieval and its relation to attention.
 */
package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place
import org.simbrain.util.setCol
import org.simbrain.util.setRow
import org.simbrain.util.updateAction
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A small modern Hopfield network built from a query layer, a softmax memory-selection layer, and a value readout.
 */
val modernHopfieldSim = newSim {
    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Modern Hopfield Network")
    val network = networkComponent.network
    val vectorSize = 16
    val memoryCount = 4

    val memories = listOf(
        doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0),
        doubleArrayOf(1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0),
        doubleArrayOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0),
        doubleArrayOf(1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    ).map { pattern -> pattern.map { if (it == 0.0) -1.0 else 1.0 }.toDoubleArray() }

    val query = NeuronArray(vectorSize).apply {
        label = "Query / current state"
        gridMode = true
        isRenderActivations = true
        isClamped = true
        setLocation(40.0, 180.0)
        setActivations(memories.first())
    }
    val attention = NeuronArray(memoryCount).apply {
        label = "Memory probabilities"
        updateRule = SoftmaxRule()
        circleMode = true
        verticalLayout = true
        setLocation(300.0, 180.0)
    }
    val retrieved = NeuronArray(vectorSize).apply {
        label = "Retrieved pattern"
        gridMode = true
        isRenderActivations = true
        setLocation(530.0, 180.0)
    }

    val keyWeights = WeightMatrix(query, attention).apply { label = "Stored keys" }
    val valueWeights = WeightMatrix(attention, retrieved).apply { label = "Stored values" }
    memories.forEachIndexed { memoryIndex, memory ->
        keyWeights.weights.setRow(memoryIndex, memory)
        valueWeights.weights.setCol(memoryIndex, memory)
    }

    network.addNetworkModelsAsync(query, attention, retrieved, keyWeights, valueWeights)

    var beta = 2.0

    fun retrieve() {
        val queryValues = query.activationArray
        val scale = beta / sqrt(vectorSize.toDouble())
        val logits = DoubleArray(memoryCount) { memoryIndex ->
            memories[memoryIndex].zip(queryValues).sumOf { (key, value) -> key * value } * scale
        }
        val largestLogit = logits.maxOrNull() ?: 0.0
        val unnormalized = logits.map { exp(it - largestLogit) }
        val normalizer = unnormalized.sum()
        val probabilities = unnormalized.map { it / normalizer }.toDoubleArray()
        val result = DoubleArray(vectorSize) { feature ->
            memories.indices.sumOf { memoryIndex -> probabilities[memoryIndex] * memories[memoryIndex][feature] }
        }
        attention.setActivations(probabilities)
        retrieved.setActivations(result)
    }

    retrieve()
    network.addUpdateAction(updateAction("Modern Hopfield retrieval") { retrieve() })

    addSidebarInfo(
        """
        # Modern Hopfield Network

        A [modern Hopfield network](https://en.wikipedia.org/wiki/Hopfield_network#Modern_Hopfield_networks) is an associative memory that retrieves a stored pattern by asking: *which memories look most like this query?* Rather than using the classical Hopfield network's symmetric recurrent weight matrix and binary neurons, it compares a query vector to a bank of stored keys, converts the similarities into a probability distribution, and blends the corresponding values. The stored patterns use balanced `-1` and `+1` activations so that a pattern with more bright cells has no built-in retrieval advantage.

        In one retrieval step, the network scores the stored memories against the current query, turns those scores into memory probabilities, and blends the stored patterns in proportion to those probabilities. Beta controls how selective retrieval is: a large beta makes the best matching memory dominate, while a small beta blends several related memories.

        ## Why this looks like a language model

        This is the essential computation of **attention** in a transformer language model. A token representation supplies a query; other token representations supply keys and values; softmax turns query–key similarities into attention weights; and the weighted value mixture becomes the context retrieved for the next calculation. Transformers add learned Q/K/V projections, multiple heads, residual connections, and feed-forward layers, but the lookup at their core has this modern-Hopfield form.

        Simbrain's `TransformerBlock` already performs self-attention. This beta simulation isolates the associative-memory part so the query, memory probabilities, and retrieved pattern are all visible.

        ## Control Panel

        - `Beta`: The slider and value indicator set retrieval selectivity. High values make one close memory dominate; low values blend several related memories.
        - `Use memory`: Each button loads one stored pattern into the clamped query layer.
        - `Add noise`: Corrupts the current query by flipping the sign of about one quarter of its features.
        - `Use retrieved pattern as query`: Copies the current retrieval into the query layer and immediately performs another retrieval step.

        ## What to do

        The query layer is clamped, so it remains the cue you set while the rest of the network updates. Each experiment uses Simbrain's step button to retrieve a pattern; the middle layer shows how much each memory contributes.

        ### 1. Simple retrieval

        Choose a `Use memory` button, then step the simulation. The selected stored pattern is the query, so its memory probability should dominate and the retrieved pattern should closely match it. You can instead edit the query array directly with the network tools before stepping.

        ### 2. Retrieval at different beta values

        Set `Beta`, choose a `Use memory` button, then step the simulation. High values of beta lead to sharp, winner-take-most retrieval; low values lead to soft retrieval that blends several similar memories.

        ### 3. Retrieval with noise

        Set `Beta`, choose a `Use memory` button, click `Add noise`, then step. `Add noise` flips the sign of about one quarter of the query features. Compare the memory probabilities and retrieved pattern at different beta values to see how selectivity affects recovery from a corrupted cue.

        ### 4. Iterative associative retrieval

        After a retrieval, click `Use retrieved pattern as query`. It copies the retrieved pattern to the clamped query layer and immediately runs the next retrieval. Repeat the button to make retrieval into a simple dynamical associative-memory process, analogous to repeatedly updating a classical Hopfield network toward an attractor, but using modern Hopfield retrieval rather than symmetric recurrent weights. With these small, well-separated memories, a clean cue may already be at a fixed point; begin with a noisy cue or a low beta value to make successive steps more informative.

        Suggested route:

        - Set `Beta` to `1.0` or `2.0`.
        - Click `Use memory 1`.
        - Click `Add noise`, then step the simulation once.
        - Click `Use retrieved pattern as query` repeatedly and observe the query, memory probabilities, and retrieved pattern.

        ## References

        Ramsauer, H. et al. (2021). [_Hopfield Networks is All You Need_](https://arxiv.org/abs/2008.02217).

        Vaswani, A. et al. (2017). [_Attention Is All You Need_](https://arxiv.org/abs/1706.03762).
        """.trimIndent()
    )

    withGui {
        val controlPanel = createControlPanel("Modern Hopfield Controls", 10, 10) {
            addSliderWithTextField(
                label = "Beta",
                minValue = 0.1,
                maxValue = 10.0,
                initValue = beta,
                increment = 0.1,
                showValueField = true,
                showTicks = true,
                toolTip = "Controls retrieval selectivity: high values favor one memory; low values blend similar memories."
            ) { value ->
                beta = value
            }
            addSeparator()
            memories.indices.forEach { memoryIndex ->
                addButton("Use memory ${memoryIndex + 1}") {
                    query.setActivations(memories[memoryIndex])
                }.apply {
                    toolTipText = "Loads stored memory ${memoryIndex + 1} into the clamped query layer."
                }
            }
            addButton("Add noise") {
                query.setActivations(query.activationArray.map { value ->
                    if (Random.nextDouble() < 0.25) -value else value
                }.toDoubleArray())
            }.apply {
                toolTipText = "Flips the sign of about one quarter of query features before the next retrieval step."
            }
            addSeparator()
            addButton("Use retrieved pattern as query") {
                query.setActivations(retrieved.activationArray)
                retrieve()
            }.apply {
                toolTipText = "Copies the retrieval to the query and immediately performs the next associative retrieval."
            }
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 760, 420)
    }
}
