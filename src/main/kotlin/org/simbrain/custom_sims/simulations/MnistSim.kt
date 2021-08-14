package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlinx.dl.dataset.handler.TEST_IMAGES_ARCHIVE
import org.jetbrains.kotlinx.dl.dataset.handler.extractImages
import org.jetbrains.kotlinx.dl.dataset.mnist
import org.simbrain.custom_sims.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.toGrayScaleImage
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.ThresholdOp

/**
 * Image world coupled to a neural network.
 */
val mnistSim = newSim {

    workspace.clearWorkspace()

    val mainScope = MainScope()

    val networkComponent = addNetworkComponent("Neural Network")

    val network = networkComponent.network

    val pixelNet = network.addNeuronGroup(400, point(-9.25, 95.93)).apply {
        label = "Retina"
        layout = GridLayout(50.0,50.0)
        setClamped(true)
    }
    pixelNet.applyLayout(-5, -85)

    println("Total Number of neurons: ${network.looseNeurons.size}")

    withGui {
        place(networkComponent) {
            location = point(673, 0)
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

    // Loads the mnist into a local cache, if it's not already there
    mnist()

    // TODO: Performance issues..
    mainScope.launch {
        val progressWindow = ProgressWindow(1000, "Images Loaded")
        progressWindow.setUpdateAction(0) { i ->
            progressWindow.value = i
            progressWindow.text = "Extracted $i/1000 images"
        }

        progressWindow.text = "Extracted 0/1000 images"
        progressWindow.pack()
        launch(Dispatchers.Default) {
            extractImages("cache/$TEST_IMAGES_ARCHIVE")
                .take(1000)
                .map {it.toGrayScaleImage(28,28)}
                .forEachIndexed { i, it ->
                    world.imageAlbum.addImage(it)
                    progressWindow.invokeUpdateAction(i)
                }
            progressWindow.close()
        }
    }

    // world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Swiss.gif"))
    // world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Poison.gif"))
    // world.imageAlbum.addImage(ResourceManager.getBufferedImage("odorworld/static/Fish.gif"))

    with(couplingManager) {
        createCoupling(
            iwc.world.filterCollection.currentFilter.getProducer("getBrightness"),
            pixelNet.getConsumer("forceSetActivations")
        )
        // iwc.world.filterCollection.currentFilter couple pixelNet
    }

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            for (i in 0..9) {
                addButton("Image $i") {
                    world.imageAlbum.setFrame(i)
                    workspace.iterate()
                }
            }

            addButton("Update") {
                workspace.iterate()
            }
        }
    }

}