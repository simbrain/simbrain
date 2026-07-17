package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.llm.TeachingTransformerConfig
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TextWorldComponent
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import java.io.File
import javax.swing.JScrollPane

class TinyLanguageModelOptions(var showTransformerOptions: Boolean = true) : EditableObject {

    var contextSize by GuiEditable(
        initValue = 24,
        description = "Number of tokens in a context window",
        order = 10,
    )

    var embeddingDimension by GuiEditable(
        description = "Width of the residual stream. Must be divisible by the number of attention heads.",
        initValue = 20,
        order = 20,
        conditionallyVisibleBy = TinyLanguageModelOptions::showTransformerOptions,
    )

    var numHeads by GuiEditable(
        label = "Attention heads",
        description = "Number of attention heads. Must divide the embedding dimension.",
        initValue = 4,
        order = 22,
        conditionallyVisibleBy = TinyLanguageModelOptions::showTransformerOptions,
    )

    var numLayers by GuiEditable(
        label = "Layers",
        description = "Number of transformer layers",
        initValue = 1,
        order = 24,
        conditionallyVisibleBy = TinyLanguageModelOptions::showTransformerOptions,
    )

    var hiddenSize by GuiEditable(
        initValue = 30,
        description = "Number of hidden units in each transformer layer's MLP",
        order = 25,
    )

    var trainerTextPath by GuiEditable(
        label = "Training text path",
        initValue = simulationsPath / "texts" / "casual_texting_small.txt",
        description = "Text used to train the model",
        order = 30,
        useFileChooser = true,
    )

    var testTextPath by GuiEditable(
        label = "Testing text path",
        initValue = "",
        description = "Optional separate text file for testing. If empty, training text will be split automatically.",
        order = 35,
        useFileChooser = true,
    )

    var trainTestSplit by GuiEditable(
        initValue = 0.6,
        description = "Fraction of data to use for training (0.0-1.0). Only used if no separate test file is provided.",
        order = 36,
    )

    var samplingStrategy: SamplingStrategy by GuiEditable(
        initValue = SamplingStrategy.TopP(),
        description = "How to sample from the next-token distribution to produce new tokens",
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
 * - embeddingDimension: Residual stream width (default: 20)
 * - numHeads: Attention heads; must divide embeddingDimension (default: 4)
 * - numLayers: Transformer layers (default: 1)
 * - hiddenSize: MLP hidden units (default: 30)
 * - textFile: Training text filename in simulations/texts/ (default: "casual_texting_small.txt")
 * - testFile: Optional test text filename in simulations/texts/ (default: none, auto-split training data)
 * - trainTestSplit: Fraction for training when auto-splitting (default: 0.6)
 * - usePunctuation: Whether tokenizer should include punctuation (default: true)
 * - trainingIterations: Number of training iterations to run (default: 0, no training)
 * - workspaceIterations: Number of workspace iterations to run (default: 0)
 * - enableConsoleOutput: Print debug info to console (default: false)
 * - learningRate: Learning rate for the Adam optimizer (default: 0.001)
 * - samplingStrategy: how to sample from the distribution to produce new tokens
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
            numHeads = jsonOptions.optInt("numHeads", numHeads)
            numLayers = jsonOptions.optInt("numLayers", numLayers)
            hiddenSize = jsonOptions.optInt("hiddenSize", hiddenSize)
            if (jsonOptions.has("textFile")) {
                trainerTextPath = simulationsPath / "texts" / jsonOptions.getString("textFile")
            }
            if (jsonOptions.has("testFile")) {
                testTextPath = simulationsPath / "texts" / jsonOptions.getString("testFile")
            }
            trainTestSplit = jsonOptions.optDouble("trainTestSplit", trainTestSplit)
            if (jsonOptions.has("usePunctuation")) {
                tokenizer = SimpleTokenizer(usePunctuation = jsonOptions.getBoolean("usePunctuation")) as Tokenizer<*>
            }

            // Parse sampling strategy
            val samplingStrategyStr = jsonOptions.optString("samplingStrategy", "topp")
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
    // The embedding dimension must split evenly across heads
    val embeddingDimension = options.embeddingDimension.let {
        it + (options.numHeads - it % options.numHeads) % options.numHeads
    }

    // Load training and test data
    val trainingText = File(options.trainerTextPath).readText()

    val (trainingTextFinal, testTextFinal) = if (options.testTextPath.isNotEmpty() && File(options.testTextPath).exists()) {
        // Use separate test file
        val testText = File(options.testTextPath).readText()
        trainingText to testText
    } else {
        // Can't use splitDataSet in trainingutils (which shuffles rows before splitting) because order matters, so we need a sequential split
        val allTokens = trainingText.tokenize(options.tokenizer).map { it.token }
        val splitIndex = (allTokens.size * options.trainTestSplit).toInt()

        val trainTokens = allTokens.take(splitIndex)
        val testTokens = allTokens.drop(splitIndex)

        val trainText = trainTokens.tokensToString(options.tokenizer)
        val testText = testTokens.tokensToString(options.tokenizer)
        trainText to testText
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

    // Vocabulary index of a token, resolved through the embedding (-1 for unknown tokens,
    // which the transformer embeds as zero rows)
    fun tokenId(token: String): Int = tokenEmbedding.get(token).indexOfFirst { it != 0.0 }

    fun tokenIds(text: String): IntArray =
        text.tokenize(tokenizer).map { tokenId(it.token) }.toIntArray()

    val transformer = TeachingTransformer(TeachingTransformerConfig(
        contextSize = contextSize,
        embedDim = embeddingDimension,
        numHeads = options.numHeads,
        hiddenDim = options.hiddenSize,
        vocabSize = tokenEmbedding.size,
        numLayers = options.numLayers,
    )).apply {
        label = "Transformer"
        tokenLabels = ArrayList(tokenEmbedding.tokens)
        this.learningRate = learningRate
    }
    transformer.setCorpus(
        tokenIds(trainingTextFinal),
        testTextFinal.takeIf { it.isNotEmpty() }?.let(::tokenIds),
    )
    if (transformer.trainer.testingWindows.isEmpty() && testTextFinal.isNotEmpty()) {
        withGui {
            showWarningDialog(
                "Warning: Test set is empty because there is insufficient test data.\n\n" +
                "If test data is desired, at least ${contextSize + 1} tokens are required for the test set.\n" +
                "To achieve this, consider lowering the train/test split ratio or using a larger dataset"
            )
        }
    }

    transformer.tokenizer = tokenizer
    transformer.samplingStrategy = options.samplingStrategy

    with(network) {
        addNetworkModels(transformer)
    }

    // Text World showing the transformer's sliding context window, editable while stopped
    val textWorldComponent = addTextWorld("Text Inputs")
    textWorldComponent.world.tokenEmbedding = tokenEmbedding
    textWorldComponent.world.highlightCurrentToken = false
    textWorldComponent.world.autoAdvance = false

    setupGenerationCouplings(workspace)

    withGui {
        val textWorldWidth = 401
        val textWorldHeight = 372
        // Create control panel for language model controls
        val controlPanel = createControlPanel("Language Model Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP + textWorldHeight + SIM_WINDOW_GAP) {

            addButton("Show Vocabulary") {
                val tokensText = tokenEmbedding.tokens.joinToString("\n")
                val textArea = SimbrainTextArea().apply {
                    text = tokensText
                    isEditable = false
                    rows = 20
                    columns = 40
                    lineWrap = true
                    wrapStyleWord = true
                }
                val scrollPane = JScrollPane(textArea)
                swingInvokeLater {
                    StandardDialog().apply {
                        title = "Vocabulary (${tokenEmbedding.tokens.size} tokens)"
                        contentPane = scrollPane
                        isModal = false
                        setAsDoneDialog()
                        makeVisible()
                    }
                }
            }

            addButton("Show Training Text") {
                val textArea = SimbrainTextArea().apply {
                    text = trainingTextFinal
                    isEditable = false
                    rows = 20
                    columns = 40
                    lineWrap = true
                    wrapStyleWord = true
                }
                val scrollPane = JScrollPane(textArea)
                swingInvokeLater {
                    StandardDialog().apply {
                        title = "Training Text"
                        contentPane = scrollPane
                        isModal = false
                        setAsDoneDialog()
                        makeVisible()
                    }
                }
            }

            addSeparator()

            // Temperature control with slider and text field
            addSliderWithTextField("Temperature", 0.01, 2.0, transformer.samplingTemperature, 0.01) { temp ->
                transformer.samplingTemperature = temp
            }

            addButton("Configure Sampling Strategy...") {
                val wrapper = objectWrapper("Sampling Strategy", transformer.samplingStrategy.copy() as SamplingStrategy)
                val editor = AnnotatedPropertyEditor(wrapper)
                val dialog = StandardDialog(editor).apply {
                    title = "Configure Sampling Strategy"
                    addCommitTask {
                        editor.commitChanges()
                        transformer.samplingStrategy = wrapper.editingObject as SamplingStrategy
                    }
                }
                dialog.display()
            }

            addSeparator()

            addButton("Start generation") {
                // Continues from whatever context the text world holds; falls back to the prompt
                transformer.resumeGeneration()
            }

            addButton("Stop generation") {
                transformer.stopGeneration()
            }
        }.awaitLayout()
        controlPanel.setLocation(
            controlPanel.centeredXInColumn(SIM_WINDOW_GAP, textWorldWidth),
            SIM_WINDOW_GAP + textWorldHeight + SIM_WINDOW_GAP
        )
        place(textWorldComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, textWorldWidth, textWorldHeight)
        place(networkComponent, SIM_WINDOW_GAP + textWorldWidth + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 900, 700)

        val textWorldDesktopComponent = SimbrainDesktop.getDesktopComponent(textWorldComponent)
        SimbrainDesktop.onboardingManager.showPopup(
            PopupConfig(
                title = "Language Model Prompt",
                message = "To enter a prompt, add some text here. To process your prompt through the network, click Run on the main toolbar.",
                targetComponent = textWorldDesktopComponent as javax.swing.JComponent,
                placement = PopupPlacement.BOTTOM_CENTER,
                suppressionKey = "tiny_language_model_prompt_help",
                style = PopupStyle.SUCCESS
            )
        )
    }

    transformer.location = point(0, 0)

    addSidebarInfo(
        """
        # Tiny Language Model

        A small GPT-style language model built from explicit tensor operations. This simulation demonstrates how a transformer processes text and how training changes its internals. Every intermediate value in the computation is visible: the residual stream, the attention heads, the weight matrices, and the gradients that flow backward during training.

        For a detailed walk-through on LLMs and transformer models that complements this discussion see the chapter on transformers [here](https://downloads.jeffyoshimi.net/NeuralNetworksCogsci.pdf).

        # Reading the Diagram

        The transformer's interior is a live diagram of its computation. Data flows top to bottom.

        ## The residual stream is the spine

        The column of wide tiles running down the diagram is the **residual stream** — the transformer's working memory. Each tile is a real matrix with one row per context position and one column per embedding dimension. At the top, token embeddings plus learned position vectors write the initial state (`residual in`). Each layer then reads the stream, computes a correction, and **adds** it back at the ⊕ junctions: once for attention (`residual + attn`), once for the MLP (`residual + mlp`).

        The straight vertical segments of the spine are the **skip connections**: information travels down them unchanged, which is why early information is still available at the bottom. The attention and MLP blocks are side branches that leave the spine and rejoin it.

        ## The attention heads are a deck

        The triangular tile is the attention pattern — row `i`, column `j` shows how much position `i` attends to position `j`. The upper triangle is exactly zero because of the **causal mask**: a token can only look backward, never at the future. The stacked cards behind it are the other heads; **scroll the mouse wheel over the deck to flip through heads**. Each head learns its own attention pattern.

        ## Weights, operations, and the lens

        Tiles with heavy orange borders are **weight matrices** — the model's learned parameters. They ride directly on the line that uses them: Wq/Wk/Wv sit on the curves into q/k/v, W1 and W2 on the MLP path (each followed by its thin **bias strip**, showing the actual bias vector), Wo on the attention output, and the unembedding on the way into the logits. They only change during training.

        The small circled icons on the connecting lines are the **operations**: ⊕ addition, × matrix multiply, a bell curve for layer norm, σ for the masked softmax, and a target for the cross-entropy loss. The activation function appears as a corner badge on the tile it produces (the hockey-stick icon on `hidden` is the ReLU). Hover over any icon to see the operation's input and output tensors.

        The small readouts beside each spine tile are the **logit lens**: each one pushes that residual state through the model's own output head and shows the token it would predict from there. Watch the prediction sharpen as you read down the spine — the bottom reading is the model's actual prediction.

        The `next-token probabilities` tile at the bottom holds one probability distribution per row. After training you will see it develop crisp structure.

        # What to Do

        ## Train the model

        The model starts untrained. Right-click the transformer's title tab and choose **Train...**, then click `Train` and watch the loss curve fall. Stop when it flattens. The weight tiles visibly change as training runs, and the attention deck develops structure.

        ## Generate text

        1. Type a prompt in the `Text Inputs` component
        2. Click `Play` (or `Step`) in the main toolbar
        3. Each workspace step runs the full transformer on the current context and samples one new token

        Tokens you type that aren't in the vocabulary become zero rows in the input (the vocabulary comes from the training text — see `Show Vocabulary`).

        ## Step through the computation, one operation at a time

        This is the heart of the simulation. Right-click the transformer:

        - **Step forward pass one op** runs a single operation of the forward pass. The active operation's glyph lights up, and every tile the computation hasn't reached yet is dimmed. Step repeatedly and watch values flow from the embeddings down to the probabilities.
        - **Step training one op** walks an entire training step: the forward pass op by op, then the loss, then **every gradient in reverse order** back down the same diagram, and finally the optimizer update (weight tiles flash as they change). Turn on **Gradient view** to see the gradient values themselves flowing backward through the tiles.
        - **Finish current step walk** completes a walk you started.

        ## Explore

        - **Double-click a tile** to trace its data-flow paths through the diagram; double-click again to clear.
        - **Hover over any cell** to read its exact value; drag tiles to rearrange the diagram.
        - Adjust `Temperature` and the sampling strategy in the control panel and compare generated text.
        - If the diagram is too large next to other components, lower **Diagram scale** in the transformer's Settings — tiles and spacing shrink while labels stay readable.
        - Try more heads or layers in the startup dialog. With two layers you can watch the logit lens improve across both.

        ## Saving

        Save the workspace to preserve the trained weights — they are stored in the workspace file and restored exactly on reopening.

        # Credits

        This simulation was developed with funding support from [UC Online](https://uconline.edu/).

        Designed by Jeff Yoshimi and Yulin Li. Thanks to Sergio Ponce de Leon, Ben Fried, and many students at UC Merced for helping with the design.

        """.trimIndent()
    )

    // Run training and workspace iterations if specified (for headless mode)
    if (trainingIterations > 0 || workspaceIterations > 0) {

        if (enableConsoleOutput) {
            println("Starting headless execution...")
            println("Training iterations: $trainingIterations")
            println("Workspace iterations: $workspaceIterations")
            println("Learning rate: $learningRate")
            println("Context size: $contextSize")
            println("Embedding dimension: $embeddingDimension")
            println("Heads: ${options.numHeads}, layers: ${options.numLayers}")
            println("Training windows: ${transformer.trainer.trainingWindows.size}")
            println("Test windows: ${transformer.trainer.testingWindows.size}")
            println()
        }

        try {
            if (trainingIterations > 0) {
                if (enableConsoleOutput) println("Starting training...")
                runBlocking {
                    repeat(trainingIterations) { iteration ->
                        transformer.trainer.trainOnce()
                        if (enableConsoleOutput) {
                            val accuracy = transformer.trainer.lastTrainingAccuracy
                                ?.let { ", Accuracy: ${"%.1f".format(it * 100)}%" } ?: ""
                            println("Iteration ${iteration + 1}/$trainingIterations, " +
                                "Train Loss: ${"%.6f".format(transformer.trainer.lastTrainingError)}$accuracy")
                        }
                    }
                }
                if (enableConsoleOutput) println("Training completed.")
            }

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
            println("Final training loss: ${"%.6f".format(transformer.trainer.lastTrainingError)}")
        }
    }

}.registerReopenFunction { workspace ->
    setupGenerationCouplings(workspace)
}

/**
 * Wires the transformer's context window to the text world as a two-way document sync: the
 * sliding window streams into the text world while generating, and text typed there while the
 * run is stopped replaces the transformer's context. The transformer samples and feeds back
 * its own tokens, so the old hand-ordered update actions are gone — the default workspace
 * update (couplings, then components) drives everything. Recreating existing couplings on
 * reopen is safe: the coupling manager stores them in a set.
 */
fun SimulationScope.setupGenerationCouplings(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val transformer = network.getModels<TeachingTransformer>().first()
    val textWorld = workspace.componentList.filterIsInstance<TextWorldComponent>().first().world

    with(workspace.couplingManager) {
        createCoupling(
            textWorld.getProducer("getText"),
            transformer.getConsumer("setContextWindow"),
        )
        createCoupling(
            transformer.getProducer("getContextWindow"),
            textWorld.getConsumer("setTextIfChanged"),
        )
    }
}
