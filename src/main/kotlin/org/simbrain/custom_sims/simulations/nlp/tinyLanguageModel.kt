package org.simbrain.custom_sims.simulations.nlp

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import java.io.File
import kotlin.math.min

val tinyLanguageModel = newSim {

    val options = TinyLanguageModelOptions().showAPEOptionDialog("Tiny Language Model") ?: return@newSim

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    val hiddenLayerSize = 100

    val trainingText = File(options.trainerTextPath).readText()

    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
        tokenizePunctuation = true
    }.build(trainingText)

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = trainingText.simpleTokenizer(options.useSpaces, options.usePunctuation)
    val corpus = tokenizedTrainingText.windowed(min(tokenizedTrainingText.size, contextSize)).flatMap { window ->
        // window along the tokens if the context size is not big enough to cover the entire token list
        generateAutoregressivePairs(window)
    }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.text = tokenizedTrainingText.take(contextSize).joinToString(if (options.useSpaces) "" else " ")
    textWorldComponent.world.tokenEmbedding = tokenEmbedding

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
            trainingSet = MatrixDataset(
                inputs = inputMatrix,
                targets = targetMatrix
            )
            (hiddenLayers().first().updateRule as? SigmoidalRule)?.apply {
                lowerBound = -1.0
            }
            outputLayer.updateRule = SoftmaxRule()
            inputLayer.gridMode = true
            inputLayer.location += point(0, 100)
        }.addToNetwork()
    }

    backpropNetwork.trainer.apply {
        lossFunction = BackpropLossFunction.CrossEntropy
        learningRate = 0.0001
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
        textWorldComponent.world.text = textWorldComponent.world.text.simpleTokenizer(useSpaces = options.useSpaces, usePunctuation = options.usePunctuation)
            .plus(nextWord)
            .takeLast(contextSize)
            .joinToString(if (options.useSpaces) "" else " ")
    }

    withGui {
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 500, 550)
    }

}

