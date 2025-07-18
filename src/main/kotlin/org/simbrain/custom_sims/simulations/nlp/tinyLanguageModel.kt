package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.updater.UpdateAllCouplings
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TextWorldComponent
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import java.io.File

/**
 * Creates sequence-to-sequence training data for proper GPT-style training.
 * 
 * For input sequence ["hi", "there", "old", "friend"], creates:
 * Input:  Matrix(contextSize, vocabSize) for each training example
 * Target: Matrix(contextSize, vocabSize) for each training example (shifted by 1)
 * 
 * Each position learns to predict the next token simultaneously.
 */
fun buildSequenceToSequenceDataset(
    tokenizedText: List<String>, 
    contextSize: Int, 
    tokenEmbedding: TokenEmbedding
): MatrixDataset {
    
    // Create sliding windows of contextSize + 1 for input + target
    val sequences = tokenizedText.windowed(contextSize + 1, step = 1)
    
    val numSequences = sequences.size
    val vocabSize = tokenEmbedding.dimension
    
    // Each training example is a matrix: (contextSize, vocabSize)
    val allInputMatrices = mutableListOf<Matrix>()
    val allTargetMatrices = mutableListOf<Matrix>()
    
    sequences.forEach { window ->
        val inputTokens = window.dropLast(1)  // First contextSize tokens
        val targetTokens = window.drop(1)     // Last contextSize tokens (shifted by 1)
        
        // Create input matrix for this training example
        val inputMatrix = Matrix(contextSize, vocabSize)
        inputTokens.forEachIndexed { positionIndex, token ->
            val oneHot = tokenEmbedding.get(token)
            inputMatrix.setRow(positionIndex, oneHot)
        }
        
        // Create target matrix for this training example  
        val targetMatrix = Matrix(contextSize, vocabSize)
        targetTokens.forEachIndexed { positionIndex, token ->
            val oneHot = tokenEmbedding.get(token)
            targetMatrix.setRow(positionIndex, oneHot)
        }
        
        allInputMatrices.add(inputMatrix)
        allTargetMatrices.add(targetMatrix)
    }
    
    // Convert to the format expected by MatrixDataset
    // Each row in the final matrices represents one training example (flattened)
    val finalInputMatrix = Matrix(numSequences, contextSize * vocabSize)
    val finalTargetMatrix = Matrix(numSequences, contextSize * vocabSize)
    
    allInputMatrices.forEachIndexed { exampleIndex, inputMatrix ->
        finalInputMatrix.setRow(exampleIndex, inputMatrix.flatten())
    }
    
    allTargetMatrices.forEachIndexed { exampleIndex, targetMatrix ->
        finalTargetMatrix.setRow(exampleIndex, targetMatrix.flatten())
    }
    
    return MatrixDataset(
        inputs = finalInputMatrix,
        targets = finalTargetMatrix
    )
}

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
        initValue = simulationsPath / "texts" / "casual_texting_small.txt",
        description = "Text used to train the model",
        tab = "Text Parsing",
        order = 10,
        useFileChooser = true,
    )

    var tokenizer by GuiEditable(
        initValue = SimpleTokenizer(usePunctuation = true) as Tokenizer<*>,
        description = "Options for tokenizing the text",
        tab = "Text Parsing",
        order = 20,
    )
}

/**
 * To run from command line, use:
 * `gradle runSim -PsimName="Tiny language model" -PoptionString='{"contextSize": 12, "embeddingDimension": 16, "textFile": "chess.txt", "trainingIterations": 100, "enableConsoleOutput": true}'`
 * 
 * Available options:
 * - contextSize: Number of tokens in context window (default: 24)
 * - embeddingDimension: Vector embedding dimensions (default: 20)  
 * - textFile: Training text filename in simulations/texts/ (default: "casual_texting_small.txt")
 * - usePunctuation: Whether tokenizer should include punctuation (default: true)
 * - trainingIterations: Number of training iterations to run (default: 0, no training)
 * - workspaceIterations: Number of workspace iterations to run (default: 0)
 * - enableConsoleOutput: Print debug info to console (default: false)
 * - learningRate: Learning rate for Adam optimizer (default: 0.001)
 */
val tinyLanguageModel = newSim("tiny_language_model") { optionString ->

    // Training and debugging parameters
    var trainingIterations = 0
    var workspaceIterations = 0
    var enableConsoleOutput = false
    var learningRate = 0.001

    val options = if (optionString?.isNotEmpty() == true) {
        // Parse parameters from gradle
        val jsonOptions = JSONObject(optionString)
        trainingIterations = jsonOptions.optInt("trainingIterations", 0)
        workspaceIterations = jsonOptions.optInt("workspaceIterations", 0)
        enableConsoleOutput = jsonOptions.optBoolean("enableConsoleOutput", false)
        learningRate = jsonOptions.optDouble("learningRate", 0.001)
        
        TinyLanguageModelOptions().apply {
            contextSize = jsonOptions.optInt("contextSize", contextSize)
            embeddingDimension = jsonOptions.optInt("embeddingDimension", embeddingDimension)
            if (jsonOptions.has("textFile")) {
                trainerTextPath = simulationsPath / "texts" / jsonOptions.getString("textFile")
            }
            if (jsonOptions.has("usePunctuation")) {
                tokenizer = SimpleTokenizer(usePunctuation = jsonOptions.getBoolean("usePunctuation")) as Tokenizer<*>
            }
        }
    } else {
        // Use GUI dialog for interactive mode
        TinyLanguageModelOptions().showAPEOptionDialog("Tiny Language Model") ?: return@newSim
    }

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    val trainingText = File(options.trainerTextPath).readText()

    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.OneHot()
        tokenizer = options.tokenizer
    }.build(trainingText)

    val tokenizer by tokenEmbedding::tokenizer

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val tokenizedTrainingText = trainingText.tokenize(tokenizer).map { it.token }
    val trainingSet = buildSequenceToSequenceDataset(tokenizedTrainingText, contextSize, tokenEmbedding)

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text World (Inputs)")
    textWorldComponent.world.tokenEmbedding = tokenEmbedding
    textWorldComponent.world.text = tokenizedTrainingText.take(contextSize).tokensToString(tokenizer)
    textWorldComponent.world.highlightCurrentToken = false
    textWorldComponent.world.autoAdvance = false

    val inputs = ActivationSequence(contextSize, tokenEmbedding.dimension).apply {
        label = "Inputs"
        isClamped = true
    }

    val transformerBlock = TransformerBlock(contextSize, options.embeddingDimension, options.embeddingDimension).apply {
        label = "Transformer Block"
    }

    // Sequence-to-sequence softmax layer for proper GPT training
    val softmaxSequence = ActivationSequence(contextSize, tokenEmbedding.dimension).apply {
        updateRule = SoftmaxRule().apply {
            temperature = 0.2
        }
        label = "Softmax Sequence (Training)"
    }

    // Separate inference layer for update actions (maintains old behavior)
    val inferenceOutput = NeuronArray(tokenEmbedding.dimension).apply {
        circleMode = size < 100
        gridMode = true
        labelArray = tokenEmbedding.tokens.toTypedArray()
        label = "Predicted Next Token (Inference)"
    }

    val weightMatrices = listOf(
        WeightMatrix(inputs, transformerBlock),
        WeightMatrix(transformerBlock, softmaxSequence)
        // Note: No weight matrix to inferenceOutput - handled by custom update action
    )

    with(network) {
        addNetworkModels(inputs, transformerBlock, softmaxSequence, inferenceOutput).awaitAll()
        addNetworkModels(weightMatrices).awaitAll()
        val model = SupervisedModel(inputs, softmaxSequence) // Train on sequence, not single output
        model.initWeights()
        model.initBiases()
        model.trainingSet = trainingSet
        model.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        model.trainerConfig.learningRate = learningRate
        model.trainerConfig.testConfiguration.enabled = false
        model.trainerConfig.optimizer = AdamOptimizer()
        addNetworkModels(model).awaitAll()
    }

    setupUpdateActions(workspace)

    inputs.location = point(-625, -200)
    transformerBlock.location = point(-300, -600)
    softmaxSequence.location = point(-1000, -600)
    inferenceOutput.location = point(-1000, -400)

    withGui {
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

    // Run training and workspace iterations if specified (for headless mode)
    if (trainingIterations > 0 || workspaceIterations > 0) {
        
        if (enableConsoleOutput) {
            println("Starting headless execution...")
            println("Training iterations: $trainingIterations")
            println("Workspace iterations: $workspaceIterations")
            println("Learning rate: $learningRate")
            println("Context size: $contextSize")
            println("Embedding dimension: ${options.embeddingDimension}")
            println("Text file: ${options.trainerTextPath}")
            println()
        }

        // Get components for headless execution
        val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
        val supervisedModel = network.getModels<SupervisedModel>().first()
        val trainer = SupervisedTrainer(network, supervisedModel)
        val softmaxSequence = network.getModelByLabel<ActivationSequence>("Softmax Sequence (Training)")
        val inferenceOutput = network.getModelByLabel<NeuronArray>("Predicted Next Token (Inference)")
        
        try {
            // Run training iterations
            if (trainingIterations > 0) {
                if (enableConsoleOutput) println("Starting training...")
                repeat(trainingIterations) { iteration ->
                    trainer.trainOnce()
                    
                    if (enableConsoleOutput) {
                        // Run a forward pass to get current activations
                        with(network) {
                            supervisedModel.forwardPass()
                        }
                        
                        // Copy sequence output to inference output for visualization
                        val lastMeaningfulIndex = (contextSize - 1).coerceAtLeast(0)
                        val sequenceOutput = softmaxSequence.activations.row(lastMeaningfulIndex)
                        
                        // Print error for every iteration
                        println("Iteration ${iteration + 1}/$trainingIterations, Loss: ${"%.6f".format(trainer.lastTrainingError)}")
                        
                        // Sample activations every iteration
                        println("  Softmax Sequence activations (first 3 positions, first 5 tokens):")
                        for (pos in 0 until minOf(3, softmaxSequence.sequenceSize)) {
                            val positionActivations = (0 until minOf(5, softmaxSequence.size)).map { 
                                softmaxSequence.activations[pos, it] 
                            }
                            println("    Position $pos: [${positionActivations.joinToString(", ") { "%.3f".format(it) }}...]")
                        }
                        
                        // Show predicted next token probabilities
                        println("  Predicted Next Token (top 5 probabilities):")
                        val topIndices = inferenceOutput.activationArray
                            .mapIndexed { index, value -> index to value }
                            .sortedByDescending { it.second }
                            .take(5)
                        
                        topIndices.forEach { (tokenIndex, prob) ->
                            val token = if (tokenIndex < tokenEmbedding.tokens.size) {
                                tokenEmbedding.tokens[tokenIndex]
                            } else "UNK"
                            println("    '$token': ${"%.3f".format(prob)}")
                        }
                        println()
                    }
                }
                if (enableConsoleOutput) println("Training completed.")
            }

            // Run workspace iterations
            if (workspaceIterations > 0) {
                if (enableConsoleOutput) println("Starting workspace iterations...")
                repeat(workspaceIterations) { iteration ->
                    workspace.iterateSuspend()
                    if (enableConsoleOutput && (iteration + 1) % 10 == 0) {
                        println("Workspace iteration ${iteration + 1}/$workspaceIterations")
                    }
                }
                if (enableConsoleOutput) println("Workspace iterations completed.")
            }

        } catch (e: Exception) {
            println("ERROR during execution: ${e.message}")
            e.printStackTrace()
            throw e
        }

        if (enableConsoleOutput) {
            println("Headless execution completed successfully!")
            println("Final loss: ${"%.6f".format(trainer.lastTrainingError)}")
        }
    }

}.registerReopenFunction { workspace -> setupUpdateActions(workspace) }

fun SimulationScope.setupUpdateActions(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val supervisedModel = network.getModels<SupervisedModel>().first()
    val inputs = network.getModelByLabel<ActivationSequence>("Inputs")
    val softmaxSequence = network.getModelByLabel<ActivationSequence>("Softmax Sequence (Training)")
    val inferenceOutput = network.getModelByLabel<NeuronArray>("Predicted Next Token (Inference)")

    val contextSize = inputs.sequenceSize

    val textWorldComponent = workspace.componentList.filterIsInstance<TextWorldComponent>().first()
    val textWorld = textWorldComponent.world

    workspace.updater.updateManager.clear()

    workspace.addUpdateAction("Encode Context Window") {
        val encodedContext = textWorld.text
            .tokenize(textWorld.tokenizer)
            .map { textWorld.tokenEmbedding.get(it.token) }

        val contextMatrix = Matrix(contextSize, textWorld.tokenEmbedding.dimension)
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

    workspace.addUpdateAction("Copy Sequence Output to Inference") {
        // Determine the last meaningful position in the context window
        val currentTokens = textWorld.text.tokenize(textWorld.tokenizer).map { it.token }
        val actualLength = minOf(currentTokens.size, contextSize)
        val lastMeaningfulIndex = (actualLength - 1).coerceAtLeast(0)
        
        // Copy activations from the last meaningful position in the sequence to inference output
        val sequenceOutput = softmaxSequence.activations.row(lastMeaningfulIndex)
        inferenceOutput.activations = sequenceOutput.toColumnVector()
    }

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = textWorld.tokenEmbedding.getClosestWord(inferenceOutput.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        textWorldComponent.world.text = textWorldComponent.world.text.tokenize(textWorld.tokenizer)
            .map { it.token }
            .plus(nextWord)
            .takeLast(contextSize)
            .tokensToString(textWorld.tokenizer)
        textWorldComponent.world.currentTokenIndex = textWorldComponent.world.tokens.lastIndex
    }

}