/**
 * Competitive learning game: five patterns on seven input nodes, and the goal of giving each pattern its own output
 * unit. Units are labeled with the patterns they most recently won, using [WinnerLabeler]. The picture version of the
 * same game is in competitiveGridSim.kt.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.add
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.NormalDistribution

val competitiveSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val competitive = CompetitiveNetwork(7, 5)
    network.addNetworkModelAsync(competitive)
    competitive.applySimulationLayout()
    competitive.inputLayer.setUpperBound(1.0)
    
    competitive.learningRate = 0.1

    // Each pattern turns on three of the seven inputs, and any two patterns share exactly one input
    // (five lines of the Fano plane). With this little overlap, a unit trained onto one pattern
    // responds less to the others than an untrained unit does, so committing a unit releases the rest.
    val patterns = listOf(
        listOf(1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0),
        listOf(0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0),
        listOf(0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0),
        listOf(0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0),
        listOf(1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0),
    )

    var winningLabel = ""
    val labelTracker = WinnerLabeler()

    addSidebarInfo(
        """
        # Competitive Network (Simple)

        A competitive network learns to sort its inputs into groups without being told what the groups are. This simulation is a game: give each of the five patterns its own output unit. A short [interactive tutorial](https://simbrain.net/misc/competitive_game/) plays the same game and shows what happens inside the network while you play.

        Each output unit's seven fan-in weights form a point in the same 7-dimensional space as the input patterns. When a pattern is presented, the unit whose weights give it the largest weighted input wins, and only the winner's weights move toward that pattern. Each output unit is labeled with the patterns it most recently won. A label stays on a unit until that pattern is trained or tested again, so it can be out of date; press `Test` on each pattern to check.

        # Simulation Details

        Each pattern turns on three of the seven input nodes, and any two patterns share exactly one active node. Weights start random. Inputs are normalized (divided by their sum) before learning, so a unit trained all the way onto a pattern puts a third of its weight on each of that pattern's inputs.

        A unit trained onto one pattern responds `1.0` to that pattern but only `0.33` to the others, which is less than an untrained unit gives (about `0.43`). So a unit that commits to one pattern lets go of the rest, and an unused unit can pick them up. A unit that splits its time between two patterns settles halfway between them and responds about `0.67` to both, so it keeps both.

        ## Control Panel Settings

        - `Pattern 1` to `Pattern 5`: Put that pattern on the input nodes. Nothing learns until you train.
        - `Add Noise`: Add small random values to the current input.
        - `Test`: Show which unit wins the current pattern without changing any weights.
        - `Train`: Run one training step on the current pattern. The winning unit's weights move a tenth of the way toward the input.
        - `Train ×10`: Run ten training steps on the current pattern.
        - `Reset`: Randomize the weights and clear the labels.

        # What to Do

        1. Select each pattern and press `Test` to see the starting position. Usually two or more patterns share a unit.
        2. Train the patterns in turn, one step each, for a few rounds, then test each one. Taking turns rarely works: on about one start in eight.
        3. Press `Reset` and commit instead: select a pattern and press `Train ×10` twice before moving on to the next pattern. Then test each pattern. This works about nine times in ten.
        4. If two patterns still share a unit, train one of them more until the other one moves to a new unit.

        ## Things to Try

        Right-click the competitive network and choose `Edit` to change these.

        - `Learning rate`: At `0.5`, each step jumps most of the way, so even taking turns often works. At `0.05`, committing takes twice as many steps.
        - `Normalize inputs`: Turn it off and commit as before. One unit takes every pattern, because its weights grow toward `1` on the pattern's inputs, so it responds `1.0` to every pattern that shares an input.
        - `Use Leaky learning`: Losing units drift slowly toward each input, which helps a little by bringing unused units into play.

        The `Competitive network (shapes)` simulation plays the same game with five pictures on a `10`x`10` input grid.

        # References

        Rumelhart, D. E., & Zipser, D. (1985). [_Feature discovery by competitive learning_](https://doi.org/10.1207/s15516709cog0901_5). _Cognitive Science_, _9_(1), 75-112.

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

            patterns.forEachIndexed { i, pattern ->
                addButton("Pattern ${i + 1}") {
                    competitive.inputLayer.neuronList.activations = pattern
                    winningLabel = "P${i + 1}"
                }.toolTipText = "Put pattern ${i + 1} on the input nodes"
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
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 674, 615)
    }
}
