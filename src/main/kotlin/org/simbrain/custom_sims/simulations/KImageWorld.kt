package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.util.addNeuronGroup
import org.simbrain.util.ResourceManager
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.ThresholdOp

/**
 * Image world coupled to a neural network.
 */
val imageSim = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Neural Network")

    val network = networkComponent.network

    val pixelNet = network.addNeuronGroup(400, point(-9.25, 95.93)).apply {
        label = "Retina"
        layout = GridLayout(50.0,50.0)
    }
    pixelNet.applyLayout()

    println("Total Number of neurons: ${network.looseNeurons.size}")

    withGui {
        place(networkComponent) {
            location = point(673, 0)
            width = 400
            height = 400
        }
    }

    val iwc = addImageWorld("Image World", 166, 9, 500,500)
    val world = iwc.world
    // world.setCurrentFilter("Threshold 10x10")
    val threshold400 = Filter("Threshold 20x20", world.imageAlbum, ThresholdOp(), 20, 20)
    world.filterCollection.addFilter(threshold400)

    world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Swiss.gif"))
    world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Poison.gif"))
    world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Fish.gif"))

    with(couplingManager) {
        iwc.world.filterCollection.currentFilter couple pixelNet
    }

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            addButton("Cheese") {
                world.imageAlbum.setFrame(0)
            }
            addButton("Poison") {
                world.imageAlbum.setFrame(1)
            }
            addButton("Fish") {
                world.imageAlbum.setFrame(2)
            }
            addButton("Update") {
                workspace.iterate()
            }
        }
    }

}