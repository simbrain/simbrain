package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.hebb.HopfieldTestConfig
import org.simbrain.custom_sims.simulations.hebb.createHopfieldTestPane
import org.simbrain.custom_sims.simulations.hebb.createPatternControlPanel
import org.simbrain.custom_sims.simulations.hebb.signedHammingDistance
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.updaterules.AdditiveRule
import org.simbrain.util.place
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.showNumericInputDialog

/**
 *  Demo for studying continuous Hopfield networks,
 *
 *  Includes forgetting dynamics.
 *
 *  Inspired in part by https://arxiv.org/abs/2112.00119
 */

val hopfieldSimContinuous = newSim {

    val numNeurons = showNumericInputDialog("Number of neurons", 100) ?: return@newSim

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Neurons with additive nodes

    val hopfield = network.addNeuronCollection(numNeurons) { updateRule = AdditiveRule() }.apply {
        toggleClamping() // Default to clamping for training
        betweenNeuronInterval = 25
        setLayoutBasedOnSize()
        applyLayout()
    }
    val wm = WeightMatrix(hopfield, hopfield).apply {
        learningRule = HebbianRule().apply {
            learningRate = .1
        }
        weights.randomizeSymmetric()
    }

    network.addNetworkModel(wm)

    addSidebarInfo(
        """
            # Forgetting in a Hopfield-like attractor network

            This simulation is a model to study continuous Hopfield networks in conjunction with forgetting dynamics. This model was inspired
            by Pereira-Obilinovic et al.'s work on the study of forgetting in attractor networks. To learn more about the model, see their paper below.

            # Simulation Details

            Unlike the discrete Hopfield simulation, this model uses continuous activation values and Euclidean distance when comparing recalled patterns.

            ## Control Panel Settings

            - `Clear pattern`: Sets every neuron to `-1`, giving a blank canvas to draw on with the wand tool.
            - `Random pattern`: Sets each neuron randomly to `-1` or `1`. Useful as a starting point for testing recall.
            - `Randomize weights`: Replaces the weights with symmetric random values, erasing anything the network has learned.
            - `Circle`, `Square`, `Diagonal Line`, `Cross`: Each loads one of the four built-in patterns into the network.
            - `Learning rate`: The Hebbian learning rate used when training. Larger values store a pattern in fewer presses.
            - `Train once on current pattern`: Applies one Hebbian learning step, storing whatever pattern is currently on the neurons in the weights. The anti-pattern is stored along with it. Press it repeatedly to strengthen the memory.
            - `Capacity` tab, pattern slider: Loads one of the randomly generated test patterns into the network.
            - `Capacity` tab, `Capacity Test`: Opens a dialog of test settings, then runs the test and plots the results. The settings are described in the `Discrete Hopfield` simulation's documentation.

            # What to Do

            ## Learning a Memory

            1. Click the `Randomize weights` button to get a fresh random weight matrix.

            2. Load a pattern (e.g., `Circle`, `Square`, `Diagonal Line`, `Cross`) and press `Train once on current pattern` several times.

            3. Click `Random pattern` or the `Clear pattern` button and iterate the model repeatedly to see the change in real-time.

            4. See how well the network has remembered the pattern by looking if the network can reproduce the same pattern or its anti-pattern. Do this
            multiple times by repeating step 3.

            ## Creating a Memory

            You can also create one of your own images for the network to be trained on.

            1. Start by clicking the `Clear pattern` button to get a fresh canvas.

            2. Now, click on the `Wand tool` or press `d` and then draw your image in the network.

            3. After that, press `Train once on current pattern` and repeat the steps above in the `Learning a Memory` section.

            ## Memory Capacity Testing

            This simulation includes capacity testing tools in the Capacity tab. These tools allow you to systematically study how many patterns
            the network can reliably store and retrieve, both with and without forgetting dynamics.

            The continuous Hopfield network uses the same capacity testing framework as the discrete Hopfield simulation. The main difference is
            that this version uses Euclidean distance for measuring pattern similarity (since activations are continuous values), while the discrete
            version uses Hamming distance (for binary patterns).

            For detailed information on the background, theory, and usage of the capacity tests, see the `Discrete Hopfield` simulation's documentation,
            which includes:
            - Background on Hopfield network memory capacity (the classical 0.138N limit)
            - How forgetting mechanisms (weight decay and synaptic noise) affect memory stability
            - Step-by-step explanation of how the capacity test works
            - How to interpret the results
            - References to the relevant literature

            # References

            Pereira-Obilinovic, U., Aljadeff, J., & Brunel, N. (2023). [_Forgetting Leads to Chaos in Attractor Networks_](https://doi.org/10.1103/physrevx.13.011009). _Physical Review X_, _13_(1).

            # Credits

            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

            Makenzy Gilbert

            Kanly Thao

        """.trimIndent()
    )

    var learningRate = .1
    var forgettingRate = .1

    fun initLearningRate() {
        (wm.learningRule as HebbianRule).learningRate = learningRate
    }

    fun initForgettingRate() {
        (wm.learningRule as HebbianRule).forgettingRate = forgettingRate
    }
    initLearningRate()
    initForgettingRate()

    withGui {
        val controlPanel = createPatternControlPanel(hopfield, true) {
            wm.weights.randomizeSymmetric()
            wm.events.updated.fire()
        }?.apply {
            //addTextField("Forgetting rate", "" + forgettingRate) {
            //    it.toDoubleOrNull()?.let { num ->
            //        forgettingRate = num
            //    }
            //    initForgettingRate()
            //}
            addTextField(
                "Learning rate",
                "" + learningRate,
                toolTip = "Hebbian learning rate used by Train once on current pattern."
            ) {
                it.toDoubleOrNull()?.let { num ->
                    learningRate = num
                }
                initLearningRate()
            }
            addButton("Train once on current pattern") {
                // Forces into training mode
                hopfield.isAllClamped = true
                wm.clamped = false
                // Now train
                workspace.simpleIterate()
                // Go to retrieval mode so user can test
                hopfield.isAllClamped = false
                wm.clamped = true

            }.apply {
                toolTipText = "Applies one Hebbian learning step, storing the current pattern (and its anti-pattern) in the weights."
            }
            //addSeparator()
            //addButton("Training Mode") {
            //    hopfield.isAllClamped = true
            //    wm.clamped = false
            //}
            //addButton("Retrieval Mode") {
            //    hopfield.isAllClamped = false
            //    wm.clamped = true
            //}
            val config = HopfieldTestConfig(
                workspace = workspace,
                hopfield = hopfield,
                weights = wm,
                applyTraining = {
                    // Training mode
                    hopfield.isAllClamped = true
                    wm.clamped = false
                    workspace.iterateSuspend(1)
                    // Testing mode
                    hopfield.isAllClamped = false
                    wm.clamped = true
                },
                applyLearningRate = {
                    (wm.learningRule as HebbianRule).learningRate = it
                    (wm.learningRule as HebbianRule).forgettingRate = 0.0
                },
                applyReset = {
                    hopfield.clear()
                    wm.hardClear()
                    hopfield.isAllClamped = false
                    wm.clamped = true
                },
                distanceFunction = ::signedHammingDistance
            )
            createHopfieldTestPane(config, false)

        }?.awaitLayout()
        place(networkComponent, controlPanel?.rightEdgeWithGap() ?: SIM_WINDOW_GAP, SIM_WINDOW_GAP, 509, 619)

    }

}
