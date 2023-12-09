package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.div
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.textworld.TokenEmbeddingBuilder

/**
 * Todo
 *
 */
val nlpSim_neuralNetworks = newSim {

    // Potentially implement an alternative algorithm, using a neural network trained on next word prediction
    // Export a layer weights as the word embedding
    // Comparison between the traditional count methods vs next word prediction
    // Generate text using the neural network?

    workspace.clearWorkspace()

    // Text World for Inputs
    val textWorld = addTextWorld("Text World (Inputs)")
    val text = readSimulationFileContents("texts" / "mlk.txt")
    TokenEmbeddingBuilder().build(text)
    textWorld.world.text = text

    withGui {
        place(textWorld) {
            location = point(0, 0)
            width = 450
            height = 250
        }
    }

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(
        network,
        textWorld.world.tokenEmbedding.dimension,
        10,
        textWorld.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModel(srn)

    withGui {
        place(networkComponent) {
            location = point(460, 0)
            width = 500
            height = 550
        }
    }

    // Text World for Outputs
    val textWorldOut = addTextWorld("Text World (Outputs)")
    TokenEmbeddingBuilder().build(text)

    withGui {
        place(textWorldOut) {
            location = point(0, 265)
            width = 450
            height = 250
        }
    }


    // Couple the text world to neuron collection
    with(couplingManager) {
        createCoupling(
            textWorld.world.getProducer("getCurrentVector"),
            srn.getConsumer("addInputs")
        )
        createCoupling(
            srn.getProducer("getOutputs"),
            textWorldOut.world.getConsumer("displayClosestWord")
        )
    }

}