package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addProjectionPlot2
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.util.Utils
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Todo
 *
 */
val nlpSim_corpusQuality = newSim {

    // 3. "Training set quality" (factors outside the parameters that affect performance)
    //
    // Potentially different training corpora:
    //              Small           Medium
    //  "Good"    Good small      Good Medium
    //  "Bad"      Bad small       Bad Medium
    // Main point: quality matters more than quantity.
    //
    // Bias in text corpora.
    // Less clear at this point, but have two training corpora (normal & explicit/extreme bias)
    // Demonstrate how these biases influence usage, which influences the resulting word embeddings
    //
    // Polysemy and word embeddings / catastrophic forgetting
    //
    // How to train/load two embeddings?

    workspace.clearWorkspace()

    // Text World
    val twc = addTextWorld("Text World")
    val textWorld = twc.world
    val text = Utils.readFileContents("simulations/texts/mlk.txt")
    textWorld.extractEmbedding(text)
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
    val projectionPlot = addProjectionPlot2("Activations")
    withGui {
        place(projectionPlot) {
            location = point(450, 0)
            width = 500
            height = 500
        }
    }

    with(couplingManager) {
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