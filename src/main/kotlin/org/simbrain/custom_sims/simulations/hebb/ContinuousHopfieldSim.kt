package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.updaterules.AdditiveRule
import org.simbrain.util.place
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.showAPEOptionDialog
import org.simbrain.util.showNumericInputDialog
import javax.swing.JLabel

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
        setUpdateRule(AdditiveRule())
        applyLayout()
        toggleClamping() // Default to clamping for training
    }
    val wm = WeightMatrix(hopfield, hopfield).apply {
        learningRule = HebbianRule().apply {
            learningRate = .1
        }
        weights.randomizeSymmetric()
    }

    network.addNetworkModels(hopfield, wm).awaitAll()

    addSidebarInfo(
        """
            # Simulation of forgetting in a Hopfield-like attractor network
            
            See https://arxiv.org/abs/2112.00119
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
                patternTestConfig = PatternTestConfig(),
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
            createHopfieldTestPane(config)

            // Option dialog
            class ForgettingTestOptions: EditableObject {

                var numPatterns by GuiEditable(
                    initValue = 10,
                    description = "Number of patterns to test",
                    order = 10,
                )

                var iterationsToForget by GuiEditable(
                    initValue = 10,
                    description = "Number of iterations to apply forgetting",
                    order = 30,
                )

                var tolerance by GuiEditable(
                    initValue = 5.0,
                    description = "Number of nodes that can be different and the pattern considered the same",
                    order = 40
                )

                var testIterations by GuiEditable(
                    initValue = 10,
                    description = "Number of times to iterate when testing recalled pattern",
                    order = 50
                )

            }

            // Forgetting
            addSeparator("Capacity")
            var numRecalled = 0
            val memoriesRecalled = JLabel("Memories recalled:--")
            val options = ForgettingTestOptions()
            addButton("Forgetting Test", tab = "Capacity") {
                options.showAPEOptionDialog("ForgettingTest")?.let {
                    numRecalled = forgettingTest(
                        config, wm,
                        it.numPatterns,
                        it.iterationsToForget,
                        it.tolerance,
                        it.testIterations
                    )
                    memoriesRecalled.text = "Memories recalled: $numRecalled"
                }
            }
            addComponent(memoriesRecalled, "Capacity")

        }

    }

}
