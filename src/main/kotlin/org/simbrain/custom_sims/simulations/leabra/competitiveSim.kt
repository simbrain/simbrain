package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.updaterules.PointNeuronRule
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Demo for studying point neurons
 *
 * Goal is to replicate some of this: https://github.com/CompCogNeuro/sims/tree/master/ch2/neuron
 */
val pointNeuronSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Point Neuron")
    val network = networkComponent.network

    val inputNeuron1 = Neuron().apply {
        clamped = true
        location = point(0, 0)
    }
    val inputNeuron2 = Neuron().apply {
        clamped = true
        location = point(100, 0)
    }
    val pointNeuron = Neuron(PointNeuronRule()).apply{
        location = point(50, 100)
    }
    val weight1 = Synapse(inputNeuron1, pointNeuron)
    val weight2 = Synapse(inputNeuron2, pointNeuron)
    network.addNetworkModels(inputNeuron1, inputNeuron2, pointNeuron, weight1, weight2)

    // TODO: Time Series

    // Control Panel
    withGui {
        place(networkComponent, 139, 10, 868, 619)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Pattern 1") {
                print("Hello!")
            }
            addButton("Pattern 2") {
                print("Hello 2")
            }
        }
    }
}