package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.div
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.textworld.TokenEmbeddingBuilder

/**
 * Basic word embeddings using neural networks
 */
val nlpSimBasicNN = newSim {

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
        textWorld.world.tokenEmbedding.dimension,
        10,
        textWorld.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModelAsync(srn)

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

    addSidebarInfo(
        """
        # Introduction
        
        This simulation demonstrates basic word embeddings using neural networks. 
        It uses a Simple Recurrent Network (SRN) to learn representations of words from text, showing how neural networks 
        can learn to encode semantic relationships between words.

        # Simulation Details
        
        The simulation consists of:
        - **Input Text World**: Displays the source text (MLK speech by default) and allows navigation through tokens
        - **SRN Network**: A simple recurrent network that learns word relationships through next-word prediction
        - **Output Text World**: Shows the closest predicted word based on the network's current output
        
        The network learns by trying to predict the next word in a sequence. Through this process, it develops internal representations (word embeddings) that capture semantic relationships between words.

        # What to Do
        
        1. **Run the simulation** to see the network processing text
        
        2. **Navigate through the text** using the controls in the input text world to see how different words activate the network
        
        3. **Observe the output** in the output text world to see what the network predicts as the next word
        
        4. **Train the network** by right-clicking on the SRN and selecting training options to improve predictions
        
        5. **Experiment with different texts** by loading other text files from the simulations/texts folder

        # References
        
        Elman, J. L. (1990). [Finding structure in time](https://crl.ucsd.edu/~elman/Papers/fsit.pdf). _Cognitive Science_, _14_(2), 179-211.

        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )

}