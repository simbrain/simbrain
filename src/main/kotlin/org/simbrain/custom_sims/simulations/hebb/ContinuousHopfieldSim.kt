package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.hebb.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.activations
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.updaterules.AdditiveRule
import org.simbrain.util.place
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.setSpectralRadius
import org.simbrain.util.showNumericInputDialog
import org.simbrain.util.stats.distributions.TwoValued
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
        weightMatrix.randomizeSymmetric()
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
        place(networkComponent, 220, 0, 509, 619)

        createPatternControlPanel(hopfield, true) {
            wm.weightMatrix.randomizeSymmetric()
            wm.events.updated.fire()
        }?.apply {
            addButton("Learn All Patterns") {
                wm.weightMatrix.randomizeSymmetric()
                hopfield.isAllClamped = true
                wm.clamped = false
                (wm.learningRule as HebbianRule).forgettingRate = 0.0
                //(wm.learningRule as HebbianRule).learningRate = (1/numNeurons).toDouble()
                repeat(numTrainIterations) {
                    applyCirclePattern(hopfield, true)
                    with(network) { wm.update() }
                    //wm.weightMatrix.setSpectralRadius(1.0)

                    applySquarePattern(hopfield, true)
                    with(network) { wm.update() }
                    //wm.weightMatrix.setSpectralRadius(1.0)

                    applyLinePattern(hopfield, "diagonal", true)
                    with(network) { wm.update() }
                    //wm.weightMatrix.setSpectralRadius(1.0)

                    applyCrossPattern(hopfield, true)
                    with(network) { wm.update() }
                    //wm.weightMatrix.setSpectralRadius(1.0)
                }
                initForgettingRate()
                initLearningRate()
                // Dump into retrieval mode for easy testing
                hopfield.isAllClamped = false
                wm.clamped = true
            }
            addSeparator()
            addTextField("Learning rate", "" + learningRate) {
                it.toDoubleOrNull()?.let { num ->
                    learningRate = num
                }
                initLearningRate()
            }
            addTextField("Forgetting rate", "" + forgettingRate) {
                it.toDoubleOrNull()?.let { num ->
                    forgettingRate = num
                }
                initForgettingRate()
            }
            addSeparator()
            addTextField("Training iterations", "" + numTrainIterations) {
                it.toIntOrNull()?.let { num ->
                    numTrainIterations = num
                }
            }
            addButton("Train") {
                // Forces into training mode
                hopfield.isAllClamped = true
                wm.clamped = false
                // Now train
                workspace.simpleIterate(numTrainIterations)
            }
            addButton("Forget") {
                repeat(numTrainIterations) {
                    (wm.learningRule as HebbianRule).applyForgetting(wm)
                }
            }
            addSeparator()
            addButton("Training Mode") {
                hopfield.isAllClamped = true
                wm.clamped = false
            }
            addButton("Retrieval Mode") {
                hopfield.isAllClamped = false
                wm.clamped = true
            }
        }

    }

}
