package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Todo
 *
 */
val nlpSim = newSim {

    // TODOS
    // - Adjust bounds
    // - Make a new simpler version of this for first lessons below
    // ---
    // - Unit tests of Ntree / Evaluate alternatives to NTree. Need a way to store the vectors for fast nearest
    // neighbor search / vector search / kd-tree
    //      https://cloud.google.com/blog/products/ai-machine-learning/vertex-matching-engine-blazing-fast-and-massively-scalable-nearest-neighbor-search
    // - Sammon map fails after one click when immediate moving from PCA
    // - Poor performance and occasional errors running sammon map while loading new items
    // - (Hard) Better algorithm for label display in PCA. Detect crowding and show some other way.

    // Possible lessons
    // 1. Geometric thinking (what is a vector space? what is a word embedding? how can we plot words in space?)
    //    - Start with a pre-loaded dictionary and a small set of words. See unit test example.
    // 2. Word co-occurrences and word embeddings (what is the algorithm doing? how do the parameters affect
    // performance?)
    // 3. Word embeddings and neural networks
    // 4. Shortcomings of DSM: polysemy (what happens to words with multiple meanings/senses?)
    
    // Something that generates text?

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
            height = 400
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
            width = 400
            height = 400
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