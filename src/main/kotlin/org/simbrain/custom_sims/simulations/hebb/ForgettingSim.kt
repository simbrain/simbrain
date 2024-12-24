package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.activations
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.updaterules.AdditiveRule
import org.simbrain.util.place
import org.simbrain.util.randomizeSymmetric
import org.simbrain.util.stats.distributions.TwoValued

/**
 * Model forgetting dynamics. Loosely based on https://arxiv.org/abs/2112.00119
 */

val forgettingSim = newSim {

    // TODO: Better patterns. Maybe letters. Or faces.
    // TODO: PCA

    val numNeurons = 100

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
        """.trimIndent()
    )

    val bipolarRandomizer = TwoValued(-1.0, 1.0)
    var numTrainIterations = 5
    var learningRate = .2
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
        place(networkComponent, 180, 0, 509, 619)
        createControlPanel("Control Panel", 0, 0) {
            addButton("Random Pattern") {
                hopfield.randomize(bipolarRandomizer)
            }
            addButton("Randomize weights") {
                wm.weightMatrix.randomizeSymmetric()
                wm.events.updated.fire()
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
                hopfield.isAllClamped = true
                wm.clamped = false
                workspace.simpleIterate(numTrainIterations)
            }
            addButton("Training Mode") {
                hopfield.isAllClamped = true
                wm.clamped = false
            }
            addButton("Testing Mode") {
                hopfield.isAllClamped = false
                wm.clamped = true
            }
        }
    }

}