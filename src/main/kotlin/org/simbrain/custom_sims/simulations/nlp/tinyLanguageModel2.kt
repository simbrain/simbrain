package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.simulationsPath
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.TransformerBlock
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import java.io.File
import kotlin.math.min

class TinyLanguageModelOptions: EditableObject {

    var contextSize by GuiEditable(
        initValue = 24,
        description = "Number of tokens in a context window",
        order = 10,
    )

    var embeddingDimension by GuiEditable(
        description = "Dimensions of the vector embedding. Each token is associated with a vector with this many components.",
        initValue = 20,
        order = 20,
    )

    var trainerTextPath by GuiEditable(
        initValue = simulationsPath / "texts" / "corpus_artificial_similarity.txt",
        description = "Text used to train the model",
        tab = "Text Parsing",
        order = 10,
        useFileChooser = true,
    )

    var useSpaces by GuiEditable(
        description = "Use spaces, tabs, and newlines as distinct tokens",
        initValue = false,
        tab = "Text Parsing",
        order = 20
    )

    var usePunctuation by GuiEditable(
        description = "Use punctuation as distinct tokens",
        initValue = false,
        tab = "Text Parsing",
        order = 30,
    )
}

val tinyLanguageModel2 = newSim {

    val options = TinyLanguageModelOptions().showAPEOptionDialog("Tiny Language Model") ?: return@newSim

    workspace.clearWorkspace()

    val contextSize = options.contextSize

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

    val inputs = ActivationSequence(contextSize, tokenEmbedding.dimension).apply {
        label = "Inputs"
        isClamped = true
    }

    val embeddings = ActivationSequence(contextSize, options.embeddingDimension).apply {
        label = "Embeddings"
    }

    val transformerBlock = TransformerBlock(contextSize, options.embeddingDimension, options.embeddingDimension)

    val softMaxLayer = NeuronArray(tokenEmbedding.dimension).apply {
        updateRule = SoftmaxRule()
        circleMode = size < 50
        gridMode = true
        labelArray = tokenEmbedding.tokens.toTypedArray()
        // Spaces are a hack for label issue in circle mode
        label = "Predicted Next Token"
    }

    val weightMatrices = listOf(
        WeightMatrix(inputs, embeddings),
        WeightMatrix(embeddings, transformerBlock),
        WeightMatrix(transformerBlock, softMaxLayer)
    )

    transformerBlock.randomize()
    weightMatrices.forEach { it.randomize() }

    with(network) {
        addNetworkModels(inputs, embeddings, transformerBlock, softMaxLayer).awaitAll()
        addNetworkModels(weightMatrices).awaitAll()
        val model = SupervisedModel(inputs, softMaxLayer, false)
        model.trainingSet = MatrixDataset(
            inputs = inputMatrix,
            targets = targetMatrix
        )
        model.trainer.lossFunction = BackpropLossFunction.CrossEntropy
        addNetworkModels(model).awaitAll()
    }

    workspace.addUpdateAction("Encode Context Window") {
        val encodedContext = textWorldComponent.world.text
            .simpleTokenizer(useSpaces = options.useSpaces, usePunctuation = options.usePunctuation)
            .map { tokenEmbedding.get(it) }

        val contextMatrix = Matrix(contextSize, tokenEmbedding.dimension)
        encodedContext.take(contextSize).forEachIndexed { i, vector ->
            contextMatrix.setRow(i, vector)
        }
        inputs.activations = contextMatrix
    }

    workspace.updater.updateManager.swapElements(0, 1)

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = tokenEmbedding.getClosestWord(softMaxLayer.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        textWorldComponent.world.text = textWorldComponent.world.text.simpleTokenizer(useSpaces = options.useSpaces, usePunctuation = options.usePunctuation)
            .plus(nextWord)
            .takeLast(contextSize)
            .joinToString(if (options.useSpaces) "" else " ")
    }

    inputs.location = point(-1000, -200)
    embeddings.location = point(-200, -200)
    transformerBlock.location = point(-300, -600)
    softMaxLayer.location = point(-1000, -600)

    withGui {
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 1000, 800)
    }

}

