package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Todo
 *
 */
val nlpSim_basic = newSim {

    // TODOS
    // 0. Step-by-step demonstration of the algorithm (count matrix -> PPMI transform, visualize matrix using tables)
    //
    // Co-occurrence matrix will be pre-trained and displayed on the main screen.
    // Vertical bar on the size showing radio button options for count matrix, ppmi transform
    // As students click the different options, a second co-occurrence will reflect the difference (for quick comparisons).
    //
    // Training text is displayed as well.
    // For the text reader, it will have options for skipGram and window size
    // The text highlight will reflect what options are selected. Maybe two different colors, target = red, context = blue?

    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = getResource("nlp/mlk.txt")
    textWorld.loadDictionary(text)
    textWorld.text = text

    withGui {
        place(twc) {
            location = point(0, 0)
            width = 400
            height = 500
        }
    }

    // Network
    // val networkComponent = addNetworkComponent("Network")
    // val network = networkComponent.network
    // val nc = network.createNeuronCollection(textWorld.tokenVectorMap.size).apply {
    //     label = "Vector Embeddings for Word Tokens"
    //     location = point(0, 0)
    //     layout(GridLayout())
    // }

    // withGui {
    //     place(networkComponent) {
    //         location = point(450, 0)
    //         width = 400
    //         height = 400
    //     }
    // }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
    withGui {
        place(projectionPlot) {
            location = point(450, 0)
            width = 500
            height = 500
        }
    }

    // Couple the text world to neuron collection
    with(couplingManager) {
        // createCoupling(
        //     textWorld.getProducer("getCurrentVector"),
        //     nc.getConsumer("addInputs")
        // )
        createCoupling(
            textWorld.getProducer("getCurrentVector"),
            projectionPlot.getConsumer("addPoint")
        )
        createCoupling(
            textWorld.getProducer("getCurrentToken"),
            projectionPlot.getConsumer("setLabel")
        )
    }

}