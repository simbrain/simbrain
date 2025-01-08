package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.updater.UpdateAllCouplings
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TextWorldComponent
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import java.awt.Dimension
import java.io.File
import kotlin.math.min

class TinyLanguageModelOptions(var showEmbeddingDimension: Boolean = true): EditableObject {

    var contextSize by GuiEditable(
        initValue = 24,
        description = "Number of tokens in a context window",
        order = 10,
    )

    var embeddingDimension by GuiEditable(
        description = "Dimensions of the vector embedding. Each token is associated with a vector with this many components.",
        initValue = 20,
        order = 20,
        conditionallyVisibleBy = TinyLanguageModelOptions::showEmbeddingDimension
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

    var tokenizePunctuation by GuiEditable(
        description = "Use punctuation as distinct tokens",
        initValue = false,
        tab = "Text Parsing",
        order = 30,
    )

    var tokenizeReturns by GuiEditable(
        description = "Use newlines as distinct tokens",
        initValue = false,
        tab = "Text Parsing",
        order = 40,
    )
}

val tinyLanguageModel2 = newSim {

    val options = TinyLanguageModelOptions().showAPEOptionDialog("Tiny Language Model") ?: return@newSim

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    val trainingText = File(options.trainerTextPath).readText()

    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
        tokenizePunctuation = options.tokenizePunctuation
        useSpaces = options.useSpaces
        useReturns = options.tokenizeReturns
    }.build(trainingText)

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = trainingText.simpleTokenizer(
        useSpaces = options.useSpaces,
        useReturns = options.tokenizeReturns,
        usePunctuation = options.tokenizePunctuation
    )
    val corpus = tokenizedTrainingText.windowed(min(tokenizedTrainingText.size, contextSize)).flatMap { window ->
        // window along the tokens if the context size is not big enough to cover the entire token list
        generateAutoregressivePairs(window)
    }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.text = tokenizedTrainingText.take(contextSize).tokensToString(spacesTokenized = options.useSpaces)
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

    with(network) {
        addNetworkModels(inputs, embeddings, transformerBlock, softMaxLayer).awaitAll()
        addNetworkModels(weightMatrices).awaitAll()
        val model = SupervisedModel(inputs, softMaxLayer, false)
        model.initWeights()
        model.initBiases()
        model.trainingSet = MatrixDataset(
            inputs = inputMatrix,
            targets = targetMatrix
        )
        model.trainer.lossFunction = BackpropLossFunction.CrossEntropy
        model.trainer.learningRate = .001
        model.trainer.testConfiguration.enabled = false
        addNetworkModels(model).awaitAll()
    }

    setupUpdateActions(workspace, options)

    inputs.location = point(-1000, -200)
    embeddings.location = point(-200, -200)
    transformerBlock.location = point(-300, -600)
    softMaxLayer.location = point(-1000, -600)

    withGui {
        createControlPanel("Control Panel", 10, 360) {
            addButton("Load Workspace") {
                val loadOk = loadWorkspaceZipFromFileChooser()
                if (loadOk) {
                    setupUpdateActions(workspace, options)
                }
            }
        }
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 1000, 800)
    }

    addSidebarInfo(
        """ 
        # Tiny Language Model
        A simple GPT-like model with one block and one head.
        
        # Configuration / Startup
        When you first start this script a dialog opens that allows you to set how large the context window 
        is and also to select a document you will use to train the model. 
        
        Note: The longer the document, the slower training will be.

        # Training your model
        Click the "supervised model" interaction box and use the training dialog as explained [here](https://docs.simbrain.net/docs/network/trainingNetworks.html)  

        # Using the model
        At any time you can see how well the model is doing just by running the [workspace](https://docs.simbrain.net/docs/workspace/)
        You can put partial text of any length in the text world to see how it does with it. 
        This is a "prompt". 
        
        Note that no turn-taking machinery here. The network will just keep generating text until you stop it. 
 
        # Save and Reopen
        Once you have trained your model, you can save it. 
        NOTE: When reopening you must use the `Load workspace` button in the control panel below the text world. 
        
        # Training data
        Generated by windowing along the tokens in the document used to train the model.  Windows do not respect punctuation,
        they are simply slid across the words.
        To see the document used to train the model, in the text world open the word embedding viewer and click "view embedding word source"
        For each window generate a "christmas tree" of all input / target pairs.
        Example: if the window is "hi there old friend" target pairs are
        
        - "hi there old" -> "friend"
        - "hi there" -> "old"
        - "hi" -> "there"
        
        # What to try
        - Try different training sets

        """.trimIndent(),
        initiallyOpened = false
    )

}

context(SimulationScope)
fun setupUpdateActions(workspace: Workspace, options: TinyLanguageModelOptions) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val supervisedModel = network.getModels<SupervisedModel>().first()
    val inputs = network.getModelByLabel<ActivationSequence>("Inputs")
    val softMaxLayer = network.getModelByLabel<NeuronArray>("Predicted Next Token")

    val contextSize = inputs.sequenceSize

    val textWorldComponent = workspace.componentList.filterIsInstance<TextWorldComponent>().first()
    val tokenEmbedding = textWorldComponent.world.tokenEmbedding

    workspace.updater.updateManager.clear()

    workspace.addUpdateAction("Encode Context Window") {
        val encodedContext = textWorldComponent.world.text
            .simpleTokenizer(useSpaces = options.useSpaces, useReturns = options.tokenizeReturns, usePunctuation = options.tokenizePunctuation)
            .map { tokenEmbedding.get(it) }

        val contextMatrix = Matrix(contextSize, tokenEmbedding.dimension)
        encodedContext.take(contextSize).forEachIndexed { i, vector ->
            contextMatrix.setRow(i, vector)
        }
        inputs.activations = contextMatrix
    }

    workspace.addUpdateAction(UpdateAllCouplings(workspace.updater))

    workspace.addUpdateAction("Update Text World") {
        textWorldComponent.update()
    }

    workspace.addUpdateAction("Update Network") {
        with(network) {
            supervisedModel.forwardPass()
        }
    }

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = tokenEmbedding.getClosestWord(softMaxLayer.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        textWorldComponent.world.text = textWorldComponent.world.text.simpleTokenizer(
            useSpaces = options.useSpaces,
            useReturns = options.tokenizeReturns,
            usePunctuation = options.tokenizePunctuation
        )
            .plus(nextWord)
            .takeLast(contextSize)
            .tokensToString(spacesTokenized = options.useSpaces)
    }

}