/**
 * Competitive learning game with picture patterns: five 10x10 pictures on a grid of input nodes, and the goal of
 * giving each picture its own output unit. Units are labeled with the pictures they most recently won, using
 * [WinnerLabeler]. The simple version of the same game is in competitiveSim.kt.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

val competitiveGridSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(100, 5)
    network.addNetworkModelAsync(competitive)
    competitive.applySimulationLayout()
    competitive.inputLayer.setUpperBound(1.0)
    competitive.learningRate = 0.1

    // Five 10x10 pictures; any two share only a few pixels
    val pictures = linkedMapOf(
        "O" to listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
            0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
            0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
            0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        ),
        "Δ" to listOf(
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
            0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
            1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
        ),
        "T" to listOf(
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0
        ),
        "X" to listOf(
            1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
            0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
            0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0,
            0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0,
            1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0
        ),
        "—" to listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        ),
    )
    val pictureNames = mapOf("O" to "circle", "Δ" to "triangle", "T" to "letter T", "X" to "X", "—" to "bar")

    var winningLabel = ""
    val labelTracker = WinnerLabeler()

    addSidebarInfo(
        """
            # Competitive Network (Shapes)

            The same game as `Competitive network (simple)`, with a `10`x`10` grid of input nodes and five picture patterns. For how competitive learning works and the training strategy, see the [interactive tutorial](https://simbrain.net/misc/competitive_game/) or the simple simulation's documentation.

            # Simulation Details

            The pictures are a circle (`O`), a triangle (`Δ`), a letter `T`, a thick `X` and a horizontal bar (`—`). They differ in size, from `20` active pixels (the bar) to `52` (the X), but any two share only a few pixels. As in the simple simulation, a unit trained onto one picture responds less to the others than an untrained unit does, so committing works. Inputs are normalized (divided by their sum) before the weights learn from them, which keeps each unit's weights summing to `1`. Each output unit is labeled with the pictures it most recently won. A label stays on a unit until that picture is trained or tested again, so it can be out of date; press `Test` on each picture to check.

            ## Control Panel Settings

            - `O`, `Δ`, `T`, `X`, `—`: Put that picture on the input grid. Nothing learns until you train.
            - `Add Noise`: Add small random values to the current input.
            - `Test`: Show which unit wins the current pattern without changing any weights.
            - `Train`: Run one training step on the current pattern. The winning unit's weights move a tenth of the way toward the input.
            - `Train ×10`: Run ten training steps on the current pattern.
            - `Reset`: Randomize the weights and clear the labels.

            # What to Do

            1. Train the pictures in turn, one step each, for a few rounds, then test each one. This gives every picture its own unit only about one time in seven.
            2. Press `Reset` and commit instead: select a picture and press `Train ×10` twice before moving on, then test each picture. This almost always works.
            3. Right-click the competitive network, choose `Edit`, and turn off `Normalize inputs`. Train again. One unit now wins every picture. Its weights grow toward `1` on every pixel it is trained on, so it responds to the other pictures with their full pixel overlap, far more than any untrained unit.

            # Credits

            Jasmine Lau

            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )

    suspend fun train(steps: Int) {
        repeat(steps) { workspace.iterateSuspend() }
        labelTracker.updateWinner(winningLabel, competitive.competitive.neuronList.maxBy { it.activation })
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {

            pictures.forEach { (label, pattern) ->
                addButton(label) {
                    competitive.inputLayer.neuronList.activations = pattern
                    winningLabel = label
                }.toolTipText = "Put the ${pictureNames.getValue(label)} on the input grid"
            }

            addButton("Add Noise") {
                competitive.inputLayer.activationArray = competitive.inputLayer.activationArray.add(NormalDistribution(standardDeviation = .01).sampleDouble(competitive.inputLayer.activationArray.size))
            }.toolTipText = "Add small random values to the current input"
            addButton("Test") {
                val savedLearningRate = competitive.learningRate
                val savedLeaky = competitive.useLeakyLearning
                competitive.learningRate = 0.0
                competitive.useLeakyLearning = false
                workspace.iterateSuspend()
                competitive.learningRate = savedLearningRate
                competitive.useLeakyLearning = savedLeaky
                labelTracker.updateWinner(winningLabel, competitive.competitive.neuronList.maxBy { it.activation })
            }.toolTipText = "Show which unit wins the current pattern without changing any weights"

            addSeparator()

            addButton("Train") { train(1) }.toolTipText = "Run one training step on the current pattern"
            addButton("Train ×10") { train(10) }.toolTipText = "Run ten training steps on the current pattern"

            addSeparator()

            addButton("Reset") {
                competitive.randomize()
                labelTracker.clear(competitive.competitive.neuronList)
            }.toolTipText = "Randomize the weights and clear the labels"
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 528, 728)
    }
}
