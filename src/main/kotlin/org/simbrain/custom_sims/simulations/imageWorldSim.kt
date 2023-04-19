package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Create with a recurrent neuron collection and a projection with a control panel to
 */
val imageWorldSim = newSim {


    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Input nodes
    val inputs = network.addNeuronCollection(100).apply {
        label = "Inputs"
        setClamped(true)
        layout(GridLayout(40.0, 40.0))
    }
    network.addNetworkModel(inputs)

    // Recurrent net
    val recurrentNet = network.addNeuronCollection(25).apply {
        label = "Process"
        layout(GridLayout())
        location = point (200, -400)
    }
    network.addNetworkModel(recurrentNet)
    network.connect(recurrentNet.neuronList, recurrentNet.neuronList, Sparse().apply {
        connectionDensity = .15
    })

    // Input to recurrent
    SynapseGroup2(inputs, recurrentNet, Sparse().apply {
        connectionDensity = .25
    }).also {
        network.addNetworkModel(it)
    }

    // Place network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    // Image world
    val component = addImageWorld("Image World")
    placeComponent(component,390,9,500,405)
    val imageWorld = component.world
    imageWorld.createBlankCanvas(10, 10)
    imageWorld.setCurrentFilter("Threshold 10x10")

    // Couple
    with(couplingManager) {
        createCoupling(
            imageWorld.filterCollection.currentFilter.getProducer("getBrightness"),
            inputs.getConsumer("forceSetActivations")
        )
    }

}