package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
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
        location = point(0, -40)
    }
    val inputNeuron2 = Neuron().apply {
        clamped = true
        location = point(0, 40)
    }
    val pointNeuron = Neuron(PointNeuronRule()).apply{
        location = point(50, 0)
    }
    val weight1 = Synapse(inputNeuron1, pointNeuron).apply {
        strength = 1.0
    }
    val weight2 = Synapse(inputNeuron2, pointNeuron).apply {
        strength = -1.0
    }
    network.addNetworkModels(inputNeuron1, inputNeuron2, pointNeuron, weight1, weight2, usePlacementManager = false)

    val (neuronPlot, voltageSeries) = addTimeSeries("Neuron plot", seriesNames = listOf("Voltage"))
    with(couplingManager) {
        pointNeuron couple voltageSeries
    }

    // Control Panel
    withGui {
        place(networkComponent, 181, 15, 405, 400)
        place(neuronPlot, 580, 15, 400, 400)
        createControlPanel("Control Panel", 5, 10) {

            addButton("Excitatory Input") {
                inputNeuron1.activation = 1.0
                inputNeuron2.activation = 0.0
                workspace.iterateSuspend(10)
            }
            addButton("Inhibitory Input") {
                inputNeuron1.activation = 0.0
                inputNeuron2.activation = 1.0
                workspace.iterateSuspend(10)
            }
        }

    }
}