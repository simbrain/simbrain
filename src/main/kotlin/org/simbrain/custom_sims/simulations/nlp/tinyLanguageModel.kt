package org.simbrain.custom_sims.simulations.nlp

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import java.io.File
import kotlin.math.min

class TinyLanguageModelOptions: EditableObject {
    var contextSize by GuiEditable(
        initValue = 24,
        order = 1,
    )

    var trainerTextPath by GuiEditable(
        initValue = simulationsPath / "texts" / "corpus_artificial_similarity.txt",
        order = 2,
        useFileChooser = true,
    )
}

val tinyLanguageModel = newSim {

    val options = showAPEOptionDialog("Tiny Language Model", TinyLanguageModelOptions())

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    val hiddenLayerSize = 100

    val trainingText = File(options.trainerTextPath).readText()

    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
        tokenizePunctuations = true
    }.build(trainingText)

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = trainingText.tokenizeWordsAndPunctuationFromString()
    val corpus = tokenizedTrainingText.windowed(min(tokenizedTrainingText.size, contextSize)).flatMap { window -> generateAutoregressivePairs(window) }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.text = tokenizedTrainingText.take(contextSize).joinToString(" ")
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
            outputLayer.updateRule = SoftmaxRule()
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
        textWorldComponent.world.text = (textWorldComponent.world.text.split(" ") + nextWord)
            .takeLast(contextSize)
            .joinToString(" ")
    }

    withGui {
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 500, 550)
    }

}

