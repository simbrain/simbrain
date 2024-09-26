package org.simbrain.custom_sims.simulations.nlp

import org.simbrain.custom_sims.*
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.updaterules.SigmoidalRule
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

    var useSpaces by GuiEditable(
        description = "Use spaces, tabs, and newlines as distinct tokens",
        initValue = false,
        order = 3,
    )

    var usePunctuation by GuiEditable(
        description = "Use punctuation as distinct tokens",
        initValue = false,
        order = 4,
    )

    var splitSentences by GuiEditable(
        description = "If true, only train on sentence fragments. If false, allow sequences across sentences.",
        initValue = true,
        order = 5,
    )

    var numberOfSentences by GuiEditable(
        description = "If splitSentences is true, the number of sentences to use at a time.",
        initValue = 1,
        conditionallyEnabledBy = TinyLanguageModelOptions::splitSentences,
        order = 6,
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
        tokenizePunctuation = true
    }.build(trainingText)

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = if (options.splitSentences) {
            trainingText.split("""[!?.]""".toRegex())
        } else {
            listOf(trainingText)
        }.map { it.simpleTokenizer(options.useSpaces, options.usePunctuation) }
        .filter { it.isNotEmpty() }
    val corpus = tokenizedTrainingText
        .windowed(if (options.splitSentences) options.numberOfSentences else tokenizedTrainingText.size)
        .flatMap { group -> // group of sentences or the entire text if splitSentences is false
            group.flatten().let { tokens ->
                generateAutoregressivePairs(tokens.take(min(tokens.size, contextSize)))
                // tokens.windowed(min(tokens.size, contextSize)).flatMap { window ->
                //     // window along the tokens if the context size is not big enough to cover the entire token list
                //     generateAutoregressivePairs(window)
                // }
            }
        }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.text = tokenizedTrainingText.first().take(contextSize).joinToString(if (options.useSpaces) "" else " ")
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

