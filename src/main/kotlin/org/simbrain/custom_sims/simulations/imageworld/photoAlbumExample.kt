package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.util.getFilesWithExtension
import org.simbrain.util.place

/**
 * Load image world with photo album coupled to a 100x100 neuron array.
 */
val photoAlbumExample = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Input nodes
    val inputArray = NeuronArray(100*100).apply {
        label = "Inputs"
        isClamped = true
        gridMode = true
    }
    network.addNetworkModel(inputArray)
    place(networkComponent,0,0,400,500)

    // Image world
    val component = addImageWorld("Image World")
    placeComponent(component,390,9,610,500)
    val imageWorld = component.world
    imageWorld.loadImages(getFilesWithExtension("simulations/images/Caltech101Sample", "jpg"))
    imageWorld.setCurrentFilter("Color 100x100")

    // Couple
    with(couplingManager) {
        createCoupling(
            imageWorld.filterCollection.currentFilter.getProducer(imageWorld.filterCollection.currentFilter::getBrightness),
            inputArray.getConsumer(inputArray::setActivations)
        )
    }

    // Force first image to load
    workspace.simpleIterate()

}