package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.HopfieldTestConfig
import org.simbrain.custom_sims.simulations.hebb.createHopfieldTestPane
import org.simbrain.custom_sims.simulations.hebb.createPatternControlPanel
import org.simbrain.custom_sims.simulations.hebb.signedHammingDistance
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.neurongroups.NeuronGroup
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

    val hopfield = NeuronGroup(numNeurons).apply {
        updateRule = AdditiveRule()
        toggleClamping() // Default to clamping for training
    }
    val wm = WeightMatrix(hopfield, hopfield).apply {
        learningRule = HebbianRule().apply {
            learningRate = .1
        }
        weights.randomizeSymmetric()
    }

    network.addNetworkModels(hopfield, wm)

    addSidebarInfo(
        """
            # Forgetting in a Hopfield-like attractor network
            
            This simulation is a model to study continuous Hopfield networks in conjunction with forgetting dynamics. This model was inspired 
            by Pereira-Obilinovic et al.'s work on the study of forgetting in attractor networks. To learn more about the model, see their paper below.
            
            # What to Do
            
            ## Learning a Memory
            
            1. Click the `Randomize Weights` button to get a fresh random weight matrix.
            
            2. `Train` the model on one object multiple times (e.g., `Circle`, `Square`, `Diagonal Line`, `Cross`).
            
            3. Click `Random Pattern` or the `-1 Canvas` button and iterate the model repeatedly to see the change in real-time.
            
            4. See how well the network has remembered the pattern by looking if the network can reproduce the same pattern or its anti-pattern. Do this
            multiple times by repeating step 3.
            
            ## Creating a Memory
            
            You can also create one of your own images for the network to be trained on.
            
            1. Start by clicking the `-1 Canvas` button to get a fresh canvas.
            
            2. Now, click on the `Wand tool` or press `d` and then draw your image in the network.
            
            3. After that, click the `Train` button and repeat the steps above in the `Learning a Memory` section.
            
            ## Forgetting Dynamics using the Capacity Test
            
            More info to come.
            
            # References
            
            Pereira-Obilinovic, U., Aljadeff, J., & Brunel, N. (2023). [Forgetting Leads to Chaos in Attractor Networks](https://doi.org/10.1103/physrevx.13.011009). _Physical Review X_, _13_(1).
            
            # Credits
 
            [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

            Kanly Thao
            
        """.trimIndent(),
        initiallyOpened = false
    )

    var numTrainIterations = 1
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
        place(networkComponent, 223, 0, 509, 619)

        createPatternControlPanel(hopfield, true) {
            wm.weights.randomizeSymmetric()
            wm.events.updated.fire()
        }?.apply {
            //addTextField("Forgetting rate", "" + forgettingRate) {
            //    it.toDoubleOrNull()?.let { num ->
            //        forgettingRate = num
            //    }
            //    initForgettingRate()
            //}
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addTextField("Learning rate", "" + learningRate) {
                it.toDoubleOrNull()?.let { num ->
                    learningRate = num
                }
                initLearningRate()
            }
            addButton("Train") {
                // Forces into training mode
                hopfield.isAllClamped = true
                wm.clamped = false
                // Now train
                workspace.simpleIterate(numTrainIterations)
                // Go to retrieval mode so user can test
                hopfield.isAllClamped = false
                wm.clamped = true

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
                    workspace.iterateSuspend(numTrainIterations)
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

        }

    }

}
