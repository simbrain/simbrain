package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.learningrules.HebbianRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.FrequencyColoringManager

/**
 * Demo for studying Hebbian Pattern association
 */
val hebbianAssociator = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val recurrentNet = network.addNeuronCollection(49).apply {
        label = "Neurons"
        layout(GridLayout(40.0, 40.0))
    }
    network.addNetworkModel(recurrentNet)
    val recurrentWeights = network.connect(recurrentNet.neuronList, recurrentNet.neuronList, AllToAll())
    recurrentWeights.forEach { s ->
        s.learningRule = HebbianRule().apply {
            learningRate = .1
        }
    }

    withGui {
        place(networkComponent) {
            location = point(280, 10)
            width = 400
            height = 400
        }
        createControlPanel("Control Panel", 5, 10) {

            addButton("Training Mode (clamped nodes)") {
                recurrentNet.setClamped(true)
                recurrentWeights.forEach { it.frozen = false }
            }.apply {
                toolTipText = "Clamps nodes and unclamps weights"
            }
            addButton("Test Mode (clamped weights)") {
                recurrentNet.setClamped(false)
                recurrentWeights.forEach { it.frozen = true }
            }.apply {
                toolTipText = "Clamps weights and unclamps nodes"
            }
            addButton("All nodes to -1") {
                recurrentNet.neuronList.forEach{n -> n.forceSetActivation(-1.0)}
            }.apply {
                toolTipText = "Provides a `palette` for creating patterns"
            }
            addTextField("Learning rate", "" + .1) {
                it.toDoubleOrNull()?.let { num ->
                    recurrentWeights.forEach { (it.learningRule as HebbianRule).learningRate = num }
                }
            }
        }
    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot2("Activations")
    projectionPlot.projector.coloringManager = FrequencyColoringManager()
    withGui {
        place(projectionPlot) {
            location = point(667, 10)
            width = 400
            height = 400
        }
    }

    // Couple the network to the projection plot
    with(couplingManager) {
        recurrentNet couple projectionPlot
    }

}