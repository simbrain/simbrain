package org.simbrain.custom_sims.simulations.nlp

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import java.io.File
import kotlin.math.min

/**
 * Initial effort to implement a language model using a feed-forward network, before we built the transformer blocks.
 */
val tinyLanguageModelFF = newSim {

    val options = TinyLmSimOptions(false).showAPEOptionDialog("Tiny Language Model") ?: return@newSim

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    val hiddenLayerSize = 100  // TODO: Add this to options

    val trainingText = File(options.trainerTextPath).readText()

    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.OneHot()
        tokenizer = options.tokenizer
    }.build(trainingText)

    val tokenizer by tokenEmbedding::tokenizer

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = tokenizer.tokenize(trainingText).map { it.token }
    val corpus = tokenizedTrainingText.windowed(min(tokenizedTrainingText.size, contextSize)).flatMap { window ->
        // window along the tokens if the context size is not big enough to cover the entire token list
        generateAutoregressivePairs(window)
    }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.tokenEmbedding = tokenEmbedding
    textWorldComponent.world.text = tokenizer.joinTokens(tokenizedTrainingText.take(contextSize))
    textWorldComponent.world.highlightCurrentToken = false
    textWorldComponent.world.autoAdvance = false

    val tokenizedCorpus = corpus.map { (context, target) ->
        context.map { tokenEmbedding.get(it) } to tokenEmbedding.get(target)
    }

    val inputMatrix = tokenizedCorpus
        .map { (context, _) -> context }
        .map {
            DoubleArray(tokenEmbedding.dimension * contextSize) { 0.0 }.also { array ->
                it.forEachIndexed { i, vector ->
                    vector.forEachIndexed { j, value ->
                        array[i * tokenEmbedding.dimension + j] = value
                    }
                }
            }
        }.toTypedArray().toMatrix()

    val targetMatrix = tokenizedCorpus.map { (_, target) -> target }.toTypedArray().toMatrix()

    val backpropNetwork = with(network) {
        BackpropNetwork(
            intArrayOf(contextSize * tokenEmbedding.dimension, hiddenLayerSize, tokenEmbedding.dimension),
        ).apply {
            label = "backprop"
            trainingSet = TrainingDataset(
                inputs = inputMatrix.toArray().map { it.toMutableList() }.toMutableList(),
                targets = targetMatrix.toArray().map { it.toMutableList() }.toMutableList()
            )
            (hiddenLayers().first().updateRule as? SigmoidalRule)?.apply {
                lowerBound = -1.0
            }
            outputLayer.updateRule = SoftmaxRule()
            inputLayer.gridMode = true
            inputLayer.location += point(0, 100)
        }.addToNetwork()
    }

    backpropNetwork.trainerConfig.apply {
        lossFunction = BackpropLossFunction.CrossEntropy
        learningRate = 0.0001
        testConfiguration.enabled = false
    }

    workspace.addUpdateAction("Encode Context Window") {
        val encodedContext = textWorldComponent.world.text
            .split(" ")
            .map { tokenEmbedding.get(it).toList() }
            .flatten()
        val inputVector = DoubleArray(tokenEmbedding.dimension * contextSize) { i ->
            encodedContext.getOrElse(i) { 0.0 }
        }
        backpropNetwork.inputLayer.setActivations(inputVector)
    }

    workspace.updater.updateManager.swapElements(0, 1)

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = tokenEmbedding.getClosestWord(backpropNetwork.outputLayer.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        textWorldComponent.world.text = textWorldComponent.world.text.tokenize(tokenizer)
            .map { it.token }
            .plus(nextWord)
            .takeLast(contextSize)
            .tokensToString(tokenizer)
    }

    withGui {
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 500, 550)
    }

    addSidebarInfo(
        """ 
        # Tiny Language Model
        A simple language model using a feed-forward network, to begin to illustrate how such models work.
        
        The input is generated by taking all each token in the context window, associating it with a one-hot encoding,
        and concatenating the results. Thus the size of the input matrix is context window * size of vocabulary. 
        
        # Basics
        You can enter an text you like in the text world and run, and it will generate text up to the size of the context 
        window, in terms of number of tokens. It will only generate up to that amount.
        
        If you only enter a few words you can get a sense of how it's working because only a few of the one-hots are visible 
        in the input.
         
        # Things this has that standard language models also have
        - The output layer is softmax and values are probabilities. 
        - The "recursive trick" is used to generate outputs
         
         # Differences
         - There is no context representation like self-attention
         
        
        

        """.trimIndent()
    )

}
