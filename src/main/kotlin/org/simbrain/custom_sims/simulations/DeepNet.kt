package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addImageWorld
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.placeComponent
import org.simbrain.network.kotlindl.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.ThresholdOp

/**
 * Create with a deep net simulation
 * TODO: Try to set up a simulation that categorizes images somehow.
 *  Maybe with all or part of mnist.
 */
val deepNetSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    // TODO: Maybe make it possible to set conv2dlayer props in constructor
    val deepNet = DeepNet(network,
        arrayListOf(TFInputLayer(100,100,1), TFConv2DLayer(), TFFlattenLayer(), TFDenseLayer(10)),
        4
    )
    network.addNetworkModel(deepNet)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }

    val iwc = addImageWorld("Image World")
    placeComponent(iwc,166,9,500,405)
    val world = iwc.world
    // world.setCurrentFilter("Threshold 10x10")

    val threshold400 = Filter("Threshold 20x20", world.imageAlbum, ThresholdOp(), 20, 20)
    world.filterCollection.addFilter(threshold400)
    world.filterCollection.currentFilter = threshold400
    withGui {
        place(iwc) {
            location = point(410, 0)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    // with(couplingManager) {
    //     neuronArray couple projectionPlot
    // }

}