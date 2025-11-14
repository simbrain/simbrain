package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.plus
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toColumnVector

/**
 * Demo for studying restricted Boltzmann machines.
 */
val rbmSim = newSim {

    val rbmCreator = RestrictedBoltzmannMachine.RBMCreator().apply {
        numVisible = 100
        numHidden = 150
    }.showAPEOptionDialog ("Create RBM Sim")
    val rbm = rbmCreator?.create()?:return@newSim

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    network.addNetworkModelAsync(rbm)

    addSidebarInfo(
        """
            # RBM

            Simulation of a restricted Boltzmann machine. RBMs are unsupervised learning models that can learn hidden representations of data and perform pattern completion.

            # Simulation Details

            This simulation contains an RBM with a visible layer (100 neurons by default) and a hidden layer (150 neurons by default). The network is pre-configured with four training patterns: circle, square, diagonal line, and cross.

            The visible layer is arranged in a 10x10 grid, making it easy to visualize patterns. The hidden layer learns feature representations of these patterns through contrastive divergence learning.

            # What to Do

            Click `Train On All Patterns` to train the RBM on all four built-in patterns. The network will iterate through each pattern multiple times based on the `Training iterations` parameter.

            To test the trained network:
            - Click on the visible layer to select it
            - Press `R` to randomize the activations
            - Press spacebar repeatedly to let the network reconstruct one of the learned patterns from the random input

            ## Experiment

            - Try training individual patterns by clicking a pattern button, then clicking `Train on current pattern`
            - Train different patterns by different amounts to see how well they are remembered
            - Use the `Add noise` button to add noise to the visible layer and observe pattern completion
            - Adjust `Training iterations` to see how learning quality changes with more or fewer iterations

            ## Creating Custom Patterns

            To create your own patterns for training:
            1. Right-click on the visible layer and select `Add coupled image world`
            2. In the Image World, load images or draw custom patterns
            3. The pattern will automatically appear in the visible layer
            4. Right-click on the RBM network and select `Add Current Pattern to Training Data`
            5. Repeat for each custom pattern you want to train on
            6. Use `Train On All Patterns` or `Train on current pattern` as before

        """.trimIndent()
    )

    withGui {
        place(networkComponent, 230, 0, 815, 619)
        var numTrainIterations = 100
        createPatternControlPanel(
            rbm.visibleLayer,
            false
        ) { rbm.randomizeWeights() }?.apply {
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train On All Patterns") {
                with(network) {
                    repeat(numTrainIterations) {
                        applyCirclePattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                        applySquarePattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                        applyLinePattern(rbm.visibleLayer, "diagonal")
                        rbm.trainOnCurrentPattern()
                        applyCrossPattern(rbm.visibleLayer)
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addSeparator()
            addButton("Train on current pattern") {
                with(network) {
                    repeat(numTrainIterations) {
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addSeparator()
            addButton("Add noise") {
                rbm.visibleLayer.activations += NormalDistribution(standardDeviation = .1)
                    .sampleDouble(rbm.visibleLayer.size)
                    .toColumnVector()
            }
        }
    }
}