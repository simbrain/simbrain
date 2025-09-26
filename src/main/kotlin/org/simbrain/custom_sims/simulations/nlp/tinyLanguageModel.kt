package org.simbrain.custom_sims.simulations.nlp

import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.UpdateAllCouplings
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TextWorldComponent
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import java.io.File

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

    var hiddenSize by GuiEditable(
        initValue = 30,
        description = "Number of hidden units in the transformer block",
        order = 25,
    )

    var trainerTextPath by GuiEditable(
        initValue = simulationsPath / "texts" / "casual_texting_small.txt",
        description = "Text used to train the model",
        order = 30,
        useFileChooser = true,
    )

    var testTextPath by GuiEditable(
        initValue = "",
        description = "Optional separate text file for testing. If empty, training text will be split automatically.",
        order = 35,
        useFileChooser = true,
    )

    var trainTestSplit by GuiEditable(
        initValue = 0.8,
        description = "Fraction of data to use for training (0.0-1.0). Only used if no separate test file is provided.",
        order = 36,
    )

    var weightDecay by GuiEditable(
        initValue = 0.01,
        description = "L2 weight decay regularization strength",
        order = 39,
        tab = "Regularization"
    )

    var learningRateDecay by GuiEditable(
        initValue = 0.001,
        description = "Learning rate decay factor per iteration (0.0 = no decay, 0.01 = moderate decay)",
        order = 40,
        tab = "Regularization"
    )

    var useAdamW by GuiEditable(
        initValue = true,
        description = "Use AdamW optimizer (decoupled weight decay) instead of Adam",
        order = 41,
        tab = "Regularization"
    )

    var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.TopK(k = 5),
        description = "How to sample from softmax to produce new tokens",
        showDetails = false,
        order = 50,
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
 * - testFile: Optional test text filename in simulations/texts/ (default: none, auto-split training data)
 * - trainTestSplit: Fraction for training when auto-splitting (default: 0.8)
 * - weightDecay: L2 weight decay strength (default: 0.01)
 * - learningRateDecay: Learning rate decay factor (default: 0.001)
 * - useAdamW: Use AdamW optimizer instead of Adam (default: true)
 * - usePunctuation: Whether tokenizer should include punctuation (default: true)
 * - trainingIterations: Number of training iterations to run (default: 0, no training)
 * - workspaceIterations: Number of workspace iterations to run (default: 0)
 * - enableConsoleOutput: Print debug info to console (default: false)
 * - learningRate: Learning rate for Adam optimizer (default: 0.001)
 * - samplingStrategy: how to sample from softmax to produce new tokens
 * - temperature: Temperature for sampling (default: 1.0)
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
            hiddenSize = jsonOptions.optInt("hiddenSize", hiddenSize)
            if (jsonOptions.has("textFile")) {
                trainerTextPath = simulationsPath / "texts" / jsonOptions.getString("textFile")
            }
            if (jsonOptions.has("testFile")) {
                testTextPath = simulationsPath / "texts" / jsonOptions.getString("testFile")
            }
            trainTestSplit = jsonOptions.optDouble("trainTestSplit", trainTestSplit)
            weightDecay = jsonOptions.optDouble("weightDecay", weightDecay)
            learningRateDecay = jsonOptions.optDouble("learningRateDecay", learningRateDecay)
            useAdamW = jsonOptions.optBoolean("useAdamW", useAdamW)
            if (jsonOptions.has("usePunctuation")) {
                tokenizer = SimpleTokenizer(usePunctuation = jsonOptions.getBoolean("usePunctuation")) as Tokenizer<*>
            }
            
            // Parse sampling strategy
            val samplingStrategyStr = jsonOptions.optString("samplingStrategy", "topk")
            samplingStrategy = when (samplingStrategyStr.lowercase()) {
                "greedy" -> SamplingStrategy.Greedy
                "topk" -> SamplingStrategy.TopK(
                    k = jsonOptions.optInt("topK", 5),
                )
                "topp" -> SamplingStrategy.TopP(
                    p = jsonOptions.optDouble("topP", 0.9),
                )
                else -> SamplingStrategy.TopK(k = 5)
            }
        }
    } else {
        // Use GUI dialog for interactive mode
        TinyLanguageModelOptions().showAPEOptionDialog("Tiny Language Model") ?: return@newSim
    }

    workspace.clearWorkspace()

    val contextSize = options.contextSize

    // Load training and test data
    val trainingText = File(options.trainerTextPath).readText()
    
    val (trainingTextFinal, testTextFinal) = if (options.testTextPath.isNotEmpty() && File(options.testTextPath).exists()) {
        // Use separate test file
        val testText = File(options.testTextPath).readText()
        trainingText to testText
    } else {
        // Auto-split the training text
        val allTokens = trainingText.tokenize(options.tokenizer).map { it.token }
        
        // Ensure we have enough tokens for both training and testing
        if (allTokens.size < contextSize + 2) {
            println("Warning: Text is too short for train/test split. Using entire text for training only.")
            trainingText to ""
        } else {
            val splitIndex = (allTokens.size * options.trainTestSplit).toInt()
            // Ensure both splits have at least contextSize + 1 tokens
            val adjustedSplitIndex = maxOf(contextSize + 1, minOf(splitIndex, allTokens.size - contextSize - 1))
            
            val trainTokens = allTokens.take(adjustedSplitIndex)
            val testTokens = allTokens.drop(adjustedSplitIndex)
            
            val trainText = trainTokens.tokensToString(options.tokenizer)
            val testText = testTokens.tokensToString(options.tokenizer)
            trainText to testText
        }
    }

    // Build token embedding from combined vocabulary (training + test)
    val combinedText = "$trainingTextFinal $testTextFinal"
    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.OneHot()
        tokenizer = options.tokenizer
    }.build(combinedText)

    val tokenizer by tokenEmbedding::tokenizer

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Create training and test datasets
    val tokenizedTrainingText = trainingTextFinal.tokenize(tokenizer).map { it.token }
    val tokenizedTestText = if (testTextFinal.isNotEmpty()) {
        testTextFinal.tokenize(tokenizer).map { it.token }
    } else {
        emptyList()
    }
    
    val trainingSet = buildSequenceToSequenceDataset(tokenizedTrainingText, contextSize, tokenEmbedding)
    val testingSet = if (tokenizedTestText.isNotEmpty()) {
        buildSequenceToSequenceDataset(tokenizedTestText, contextSize, tokenEmbedding)
    } else {
        // Create empty test dataset with correct structure
        TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = contextSize * tokenEmbedding.dimension,
            targetSize = contextSize * tokenEmbedding.dimension
        )
    }

    // Text World for Inputs
    val textWorldComponent = addTextWorld("Text Inputs")
    textWorldComponent.world.tokenEmbedding = tokenEmbedding
    textWorldComponent.world.highlightCurrentToken = false
    textWorldComponent.world.autoAdvance = false
    textWorldComponent.world.samplingStrategy = options.samplingStrategy

    val inputs = ActivationSequence(contextSize, tokenEmbedding.dimension).apply {
        label = "Inputs"
        isClamped = true
    }

    val transformerBlock = TransformerBlock(contextSize, options.embeddingDimension, options.hiddenSize).apply {
        label = "Transformer block"
    }

    // Sequence-to-sequence softmax layer
    val softmaxSequence = ActivationSequence(contextSize, tokenEmbedding.dimension).apply {
        updateRule = SoftmaxRule().apply {
            temperature = 0.2
        }
        label = "Softmax sequence"
    }

    // Separate inference layer for update actions
    val inferenceOutput = NeuronArray(tokenEmbedding.dimension).apply {
        circleMode = size < 100
        gridMode = true
        labelArray = tokenEmbedding.tokens.toTypedArray()
        label = "Predicted next token"
        (updateRule as? LinearRule)?.let {
            it.upperBound = 1.0
            it.lowerBound = -1.0
        }
    }

    val weightMatrices = listOf(
        WeightMatrix(inputs, transformerBlock).apply { label = "Embedding" },
        WeightMatrix(transformerBlock, softmaxSequence).apply { label = "Unembedding" },
        // Note: No weight matrix to inferenceOutput - handled by custom update action
    )

    with(network) {
        addNetworkModels(inputs, transformerBlock, softmaxSequence, inferenceOutput)
        addNetworkModels(weightMatrices)
        val model = SupervisedModel(inputs, softmaxSequence) // Train on sequence, not single output
        model.initWeights()
        model.initBiases()
        model.trainingSet = trainingSet
        model.testingSet = testingSet
        model.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        model.trainerConfig.learningRate = learningRate
        model.trainerConfig.testConfiguration.enabled = testingSet.size > 0
        model.trainerConfig.testConfiguration.testFrequency = 10
        
        // Configure early stopping if test data is available
        if (testingSet.size > 0) {
            model.trainerConfig.stoppingCondition.useEarlyStopping = true
            model.trainerConfig.stoppingCondition.earlyStoppingPatience = 10
            model.trainerConfig.stoppingCondition.earlyStoppingMinDelta = 0.0
        }
        model.trainerConfig.optimizer = if (options.useAdamW) {
            AdamWOptimizer().apply {
                weightDecay = options.weightDecay
                learningRateDecay = options.learningRateDecay
            }
        } else {
            AdamOptimizer() // Vanilla Adam - no weight decay or learning rate decay
        }
        model.trainerConfig.computeAccuracy = true
        addNetworkModels(model)
    }

    setupUpdateActions(workspace, options)

    inputs.location = point(-625, -200)
    transformerBlock.location = point(-300, -600)
    softmaxSequence.location = point(-1000, -600)

    withGui {
        place(textWorldComponent, 10, 10, 450, 350)
        place(networkComponent, 460, 10, 1000, 800)
        val textWorldDesktopComponent = SimbrainDesktop.getDesktopComponent(textWorldComponent)
        SimbrainDesktop.onboardingManager.showPopup(
            PopupConfig(
                title = "Language Model Prompt",
                message = "To enter a prompt, add some text here. To process your prompt through the network, click the play button on the main toolbar.",
                targetComponent = textWorldDesktopComponent as javax.swing.JComponent,
                placement = PopupPlacement.BOTTOM_CENTER,
                suppressionKey = "tiny_language_model_prompt_help",
                style = PopupStyle.SUCCESS
            )
        )
    }

    offsetNetworkModel(inputs, transformerBlock, Direction.NORTH, transformerBlock.height / 2 + 300.0)
    alignNetworkModels(inputs, transformerBlock, Alignment.VERTICAL)
    offsetNetworkModel(transformerBlock, softmaxSequence, Direction.NORTH, transformerBlock.height / 2 + 300.0)
    alignNetworkModels(transformerBlock, softmaxSequence, Alignment.VERTICAL)

    offsetNetworkModel(transformerBlock, inferenceOutput, Direction.EAST, transformerBlock.width / 2 + 400.0)
    alignNetworkModels(transformerBlock, inferenceOutput, Alignment.HORIZONTAL)

    addSidebarInfo(
        """
        # Introduction
        
        A simple GPT-like model with one block and one head.
        
        # Configuration / Startup
        
        When you first start this script a dialog opens that allows you to set how large the context window is and also to select a document you will use to train the model.
        
        Note: The longer the document, the slower training will be.

        # Training Your Model
        
        Click the "supervised model" interaction box and use the training dialog as explained [here](https://docs.simbrain.net/docs/network/trainingNetworks.html).

        # Using the Model
        
        At any time you can see how well the model is doing just by running the [workspace](https://docs.simbrain.net/docs/workspace/). You can put partial text of any length in the text world to see how it does with it. This is a "prompt".
        
        Note that no turn-taking machinery here. The network will just keep generating text until you stop it.
 
        # Save and Reopen
        
        Once you have trained your model, you can save it. 
        
        NOTE: When reopening you must use the `Load workspace` button in the control panel below the text world.
        
        # Training Data
        
        The model uses sequence-to-sequence training with sliding windows over the training text. Each training example consists of a context window of tokens, where every position in the sequence learns to predict the next token simultaneously. 
        
        For example, if the context size is 4 and we have the text "hi there old friend", the training creates:
        - Input sequence: ["hi", "there", "old", "friend"]  
        - Target sequence: ["there", "old", "friend", "next_token"]
        
        Each position in the input learns to predict its corresponding position in the target (shifted by 1). This allows the transformer to learn next-token prediction at every position in parallel, which is more efficient than autoregressive "christmas tree" training.
        
        To see the document used to train the model, in the text world open the word embedding viewer and click "view embedding word source".
        
        # What to try
        - Try different training sets
        - Experiment with temperature and different sampling strategies:
          - **Greedy**: Always picks the most probable token (deterministic)
          - **Top-K**: Samples from the K most probable tokens (good balance)
          - **Top-P (Nucleus)**: Samples from tokens whose cumulative probability exceeds P
        - Adjust temperature on the Softmax Sequence to control randomness (higher = more random)

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
            println("Training text file: ${options.trainerTextPath}")
            if (options.testTextPath.isNotEmpty()) {
                println("Test text file: ${options.testTextPath}")
            } else {
                println("Train/test split: ${options.trainTestSplit}")
            }
            println("Training set size: ${trainingSet.size}")
            println("Test set size: ${testingSet.size}")
            println()
        }

        // Get components for headless execution
        val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
        val supervisedModel = network.getModels<SupervisedModel>().first()
        val trainer = SupervisedTrainer(network, supervisedModel)
        val softmaxSequence = network.getModelByLabel<ActivationSequence>("Softmax Sequence")
        val inferenceOutput = network.getModelByLabel<NeuronArray>("Predicted Next Token")
        
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

                        // Print training error for every iteration
                        val trainLoss = trainer.lastTrainingError
                        print("Iteration ${iteration + 1}/$trainingIterations, Train Loss: ${"%.6f".format(trainLoss)}")
                        
                        // Print test error if available and it's time to compute it
                        if (testingSet.size > 0 && (iteration + 1) % 10 == 0) {
                            val testLoss = trainer.computeTestError()
                            print(", Test Loss: ${"%.6f".format(testLoss)}")
                        }
                        println()
                        
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
            println("Final training loss: ${"%.6f".format(trainer.lastTrainingError)}")
            if (testingSet.size > 0) {
                val finalTestLoss = trainer.computeTestError()
                println("Final test loss: ${"%.6f".format(finalTestLoss)}")
            }
        }
    }

}.registerReopenFunction { workspace -> 
    // For reopen, we'll use default options since we can't access the original options
    val defaultOptions = TinyLanguageModelOptions()
    setupUpdateActions(workspace, defaultOptions) 
}

fun SimulationScope.setupUpdateActions(workspace: Workspace, options: TinyLanguageModelOptions) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val supervisedModel = network.getModels<SupervisedModel>().first()
    val inputs = network.getModelByLabel<ActivationSequence>("Inputs")
    val softmaxSequence = network.getModelByLabel<ActivationSequence>("Softmax Sequence")
    val inferenceOutput = network.getModelByLabel<NeuronArray>("Predicted Next Token")

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

    /**
     * Update the text world with the predicted next token. The output activation sequence actually predicts next tokens
     * at every position, so choose the prediction corresponding (roughly) to the current number of tokens in the context
     * window.
     */
    workspace.addUpdateAction("Copy Sequence Output to Inference") {
        val currentTokens = textWorld.text.tokenize(textWorld.tokenizer).map { it.token }
        val actualLength = minOf(currentTokens.size, contextSize)
        val tokenPosition = (actualLength - 1).coerceAtLeast(0)
        val sequenceOutput = softmaxSequence.activations.row(tokenPosition)
        inferenceOutput.activations = sequenceOutput.toColumnVector()
    }

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = textWorld.sampleToken(inferenceOutput.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        textWorldComponent.world.text = textWorldComponent.world.text.tokenize(textWorld.tokenizer)
            .map { it.token }
            .plus(nextWord)
            .takeLast(contextSize)
            .tokensToString(textWorld.tokenizer)
        textWorldComponent.world.currentTokenIndex = textWorldComponent.world.tokens.lastIndex
    }

}


/**
 * Creates sequence-to-sequence training data for proper GPT-style training.
 *
 * For input sequence ["hi", "there", "old", "friend"], creates:
 * Input: Matrix(contextSize, vocabSize) for each training example
 * Target: Matrix(contextSize, vocabSize) for each training example (shifted by 1)
 *
 * Each position learns to predict the next token simultaneously.
 */
fun buildSequenceToSequenceDataset(
    tokenizedText: List<String>,
    contextSize: Int,
    tokenEmbedding: TokenEmbedding
): TrainingDataset {

    // Validate input
    if (tokenizedText.size < contextSize + 1) {
        return TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = contextSize * tokenEmbedding.dimension,
            targetSize = contextSize * tokenEmbedding.dimension
        )
    }

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

    return TrainingDataset(
        inputs = finalInputMatrix.toArray().map { it.toMutableList() }.toMutableList(),
        targets = finalTargetMatrix.toArray().map { it.toMutableList() }.toMutableList()
    )
}
