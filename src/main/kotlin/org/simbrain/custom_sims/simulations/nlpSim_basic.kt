package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.table.SimbrainDataViewer
import javax.swing.JInternalFrame

/**
 * Todo
 *
 */
val nlpSim_basic = newSim {

    // 1. "Basics of word embeddings and co-occurrence matrices"
    //
    // TODO: Offload "Training" to textbook chapter.
    // Training:
    // For the text reader, it will have options for skipGram and window size
    // The text highlight will reflect what options are selected. Maybe two different colors, target = red, context = blue?
    //
    // Co-occurrences to vectors:
    // Columns as the actual word embeddings, illustrate how they are based on the co-occurrences
    // Maybe have them test out similarity, see that raw co-occ. don't work that well
    //
    // Post-training transformations:
    // Co-occurrence matrix will be pre-trained and displayed on the main screen.
    // Vertical bar on the side showing radio button options for count matrix, ppmi transform
    // As students click the different options, a second co-occurrence will reflect the difference (for quick comparisons).


    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = getResource("nlp/river_streams.txt") // Example: check "river" and "ocean", "river" and "stream", "lake" and "ocean", n.b. regenerate embeddings after settings change
    textWorld.loadDictionary(text)
    textWorld.text = text

    withGui {
        place(twc) {
            location = point(0, 0)
            width = 400
            height = 500
        }

        val internalFrame = JInternalFrame("Co-occurence matrix", true, true)
        internalFrame.setLocation(8, 365)
        addInternalFrame(internalFrame)
        val tableViewer = SimbrainDataViewer(textWorld.tokenVectorMap.createTableModel())
        internalFrame.contentPane = tableViewer
        internalFrame.isVisible = true
        internalFrame.pack()
        textWorld.events.tokenVectorMapChanged.on {
            tableViewer.model = textWorld.tokenVectorMap.createTableModel()
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