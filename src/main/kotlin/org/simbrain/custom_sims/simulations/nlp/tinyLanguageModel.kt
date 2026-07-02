package org.simbrain.custom_sims.simulations.nlp

import org.json.JSONObject
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.UpdateAllCouplings
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TextWorldComponent
import org.simbrain.world.textworld.TokenEmbedding
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import java.io.File
import javax.swing.JScrollPane

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
        initValue = SamplingStrategy.TopP(),
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
 * - trainTestSplit: Fraction for training when auto-splitting (default: 0.6)
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

    // Create training and test datasets
    val tokenizedTrainingText = trainingTextFinal.tokenize(tokenizer).map { it.token }
    val tokenizedTestText = if (testTextFinal.isNotEmpty()) {
        testTextFinal.tokenize(tokenizer).map { it.token }
    } else {
        emptyList()
    }
    
    val trainingSet = buildSequenceToSequenceDataset(tokenizedTrainingText, contextSize, tokenEmbedding)
    val testingSet = if (tokenizedTestText.isNotEmpty()) {
        val dataset = buildSequenceToSequenceDataset(tokenizedTestText, contextSize, tokenEmbedding)
        // Warn if test set ended up empty due to insufficient data
        if (dataset.size == 0) {
            withGui {
                showWarningDialog(
                    "Warning: Test set is empty because there is insufficient test data.\n\n" +
                    "If test data is desired, at least ${contextSize + 1} tokens are required for the test set.\n" +
                    "To achieve this, consider lowering the train/test split ratio or using a larger dataset"
                )
            }
        }
        dataset
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
        circleMode = size < 150
        if (size >= 150) {
            showWarningDialog(
                message = "Circle mode is disabled in the \"predicted next token\" window because there are so many tokens.\n To see all nodes and labels right click and select \"toggle circle mode\".",
            )
        }
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
        val model = SupervisedModel(inputs, softmaxSequence)
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
            model.trainerConfig.stoppingCondition.useEarlyStopping = false
            model.trainerConfig.stoppingCondition.earlyStoppingPatience = 10
            model.trainerConfig.stoppingCondition.earlyStoppingMinDelta = 0.0
        }
        model.trainerConfig.optimizer = if (options.useAdamW) {
            AdamWOptimizer().apply {
                weightDecay = options.weightDecay
                learningRateDecay = options.learningRateDecay
            }
        } else {
            AdamOptimizer()
        }
        model.trainerConfig.computeAccuracy = true
        addNetworkModels(model)
    }

    setupUpdateActions(workspace, options)

    withGui {
        place(textWorldComponent, 10, 10, 401, 372)
        place(networkComponent, 401, 10, 791, 622)

        // Create control panel for language model controls
        createControlPanel("Language Model Controls", 10, 370) {

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
            addSliderWithTextField("Temperature", 0.01, 2.0, (softmaxSequence.updateRule as SoftmaxRule).temperature, 0.01) { temp ->
                (softmaxSequence.updateRule as SoftmaxRule).temperature = temp
            }
            
            addButton("Configure Sampling Strategy...") {
                val wrapper = objectWrapper("Sampling Strategy", textWorldComponent.world.samplingStrategy.copy() as SamplingStrategy)
                val editor = AnnotatedPropertyEditor(wrapper)
                val dialog = StandardDialog(editor).apply {
                    title = "Configure Sampling Strategy"
                    addCommitTask {
                        editor.commitChanges()
                        // Sync the edited value back to the TextWorld
                        textWorldComponent.world.samplingStrategy = wrapper.editingObject as SamplingStrategy
                    }
                }
                dialog.display()
            }
        }

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

    inputs.location = point(-820, -245)
    transformerBlock.location = point(-300, -600)
    softmaxSequence.location = point(-820, -1130)
    inferenceOutput.location = point(-1271, -1024)

    addSidebarInfo(
        """
        # Tiny Language Model

        A simplified GPT-style language model with a single transformer block. This simulation demonstrates how neural networks can learn to predict text patterns and generate new text based on training data. The model learns from sequences of text and can generate continuations based on prompts.
        
        For a detailed walk-through on LLMs and transformer models that complements this discussion see the chapter on transformers [here](https://downloads.jeffyoshimi.net/NeuralNetworksCogsci.pdf).

        # Simulation Details

        The simulation consists of several interconnected components that work together to process and generate text.

        ## How Token Processing Works

        Understanding the detailed flow through the network helps clarify what the model learns and how it generates text:

        1. **Input encoding**: Each token in the context window is converted to a one-hot vector. These vectors stack together to form an input matrix where each row represents one token position. You can observe this in the `Inputs` layer. For each token in the context window there is a corresponding row of this layer, which should have one column in red.

        2. **Embedding**: The `Embedding` weight matrix transforms the one-hot vectors into dense representations. Each one-hot row of inputs effectively selects one row from the embedding matrix, producing a token embedding for that position. The result is a stack of token embeddings flowing into the transformer.

        3. **Self-attention mechanism**: Inside the transformer block, the input token embeddings are multiplied by three learned weight matrices (`Q`, `K`, and `V`) to produce the query (`q`), key (`k`), and value (`v`) matrices, where each row corresponds to a token position. The q and k representations are then used together to compute an attention scores matrix of size `context_window × context_window`. This is where context awareness emerges. Each entry at row `i`, column `j` represents how much token `i` attends to token `j`. A triangular mask is applied (visible as a zero pattern in the upper triangle) which prevents tokens from attending to future positions they shouldn't know about yet. For example token `3` (row `3`) can attend to tokens `0`, `1`, `2`, and `3` (columns `0-3` in row `3`), but token `1` (row `1`) can only attend to tokens `0` and `1` (columns `0-1` in row `1`), not to future tokens `2` and `3`. This creates the lower triangular pattern where later tokens can look back at earlier ones, but not forward in time. The attention scores determine how the v representations are weighted and combined. The `v` matrix carries the actual information that gets passed through based on these attention scores.

        4. **Feed-forward processing**: After attention, the transformer applies a multi-layer perceptron (MLP). This is where much of the model's background knowledge about language patterns gets encoded. The MLP learns complex transformations that capture grammatical structures, semantic relationships, and other patterns from the training data.

        5. **Unembedding**: The `Unembedding` weight matrix converts the transformer's output back into the vocabulary space. This produces a matrix where each row contains scores (logits) for predicting the next token at that position in the sequence.

        6. **Softmax sequence**: The logits pass through a softmax function with temperature control, converting them into proper probability distributions. Each row of this layer corresponds to a current token, and the columns correspond to probabilities for next tokens that sum to `1.0` across all possible tokens. The `Softmax sequence` thus contains predictions for all positions in the context window simultaneously. This is useful during training when the model learns to predict the next token at every position, but during generation only one of these predictions matters, the one corresponding to the current number of tokens in the context.

        7. **Predicted next token**: This layer serves primarily as a visualization aid. It extracts and displays the single row from the `Softmax sequence` that corresponds to the current number of tokens in the context window, that is, the row at the position from which the next token will be sampled. This makes it easier to see what the model predicts. In fact, after clicking step or play, you can verify that the last token produced in the text input window corresponds to one of the most active nodes in this layer.

        8. **Generation**: During generation, a token is sampled from this probability distribution using the configured sampling strategy. The selected token is appended to the context window, the window slides forward (dropping the oldest token), and the process repeats.

        ## Interpreting the Visual Activations

        As you run the simulation, the network displays activation patterns as colors. Understanding these patterns helps you see how the model processes text. Try clicking `Run` or `Step` and watching tokens fill in one at a time as tokens are added to observe these patterns emerge. We suggest training first.

        Most patterns are row by row, and are easy to understand when filling in tokens at first, because unused rows are associated with zero vectors. Thus we see "rows" of color filling in at the different component.

        **Zero vectors and unknown tokens**: The model uses zero vectors in two situations. First, empty positions in the context window (positions not yet filled with tokens) are represented as zero vectors. Second, and importantly, if you type tokens in the `Text Inputs` window that aren't in the vocabulary, those tokens also produce zero vectors. You can type whatever you want, but any token not in the vocabulary becomes a zero row in the `Inputs` layer with no red dot. The vocabulary is determined by the training text—only tokens that appear in the training data are recognized. You can view the complete vocabulary by clicking `View token embedding editor` in the text inputs component.

        **Inputs layer**: This is the easiest component to interpret. Each row represents one position in the context window. When a token occupies that position and is in the vocabulary, you'll see a single red dot in that row: this is the one-hot encoding. The column position of that red dot identifies the token. As the model generates more tokens, more rows fill in with red dots. Rows show all zeros (no red dots) in two cases: empty positions not yet filled, or positions containing tokens not in the vocabulary.

        **Embedding transformation**: When a column is red in a given row of the inputs, matrix multiplication effectively selects the corresponding row from the `Embedding` weight matrix. In fact this is often visible after training: you can see that the rows do seem to be distinctive. This produces a matrix of dense embeddings (one per token position) that flows into the transformer block. Zero rows in the input produce zero rows going into the transformers. These empty positions don't contribute meaningful information yet.

        **`Q`, `K`, and `V` activations**: Inside the transformer block, you'll see the same rowwise pattern. The `q` (Query), `k` (Key), and `v` (Value) matrices show rows of color only for positions that contain tokens. The number of colored rows matches the number of tokens in the current context. Empty positions remain zero.

        **Attention scores**: The attention score matrix has dimensions of `context_window × context_window` and shows how each token attends to other tokens. You'll notice a triangular pattern in the activations. This comes from causal masking, described above. As tokens are added you can see a triangle pattern emerge from the top-down. Token `4` can attend to tokens `3`, `2`, and `1`, but not tokens `4`, `5`, etc. Red in column `2` of row `4` means token `4` _should_ attend to token `2`. Changes in this matrix are also evident as training progresses (you can  open the trainer and run the trainer to see this). As you do, the patterns in this matrix change, reflecting what the model learns about which tokens are relevant to each other. (Before training you won't see much in this matrix. The attention scores are computed as softmax(`q × k^T / scale`). With random weight matrices, tokens representations tend to be relatively similar across different token pairs. After softmax normalization, this produces relatively uniform attention distributions  across the lower triangle. Each token attends somewhat equally to all previous tokens rather than focusing on specific relevant ones.)

        **MLP (feed-forward) layers**: Here you'll see colored rows for positions with tokens. However, positions without tokens show vertical bars of color rather than all zeros. This happens because of bias terms in the MLP. Even when the input is a zero vector, the bias gets added, producing non-zero activations. Each zero row receives the same bias, creating those vertical bar patterns.

        **Softmax sequence**: Here each row corresponds to a probability distribution over the tokens. This is best to compare with the inputs. In both inputs and softmax each column is a token. But in softmax the shading determines how probable it is, and there will often be a few shaded columns but often one most shaded red. Here again as tokens are added the top "fills in" and the bottom has vertical bars for empty positions. These represent the probability distributions that the softmax produces in response to the MLP's bias-driven output for zero vectors. For positions with actual tokens, you see more varied activation patterns representing learned predictions.  
        
        **Predicted next token**: As noted above, this layer serves primarily as a visualization aid. It basically displays the row of `Softmax sequence` that corresponds to the current prediction. You can check that the last token produced in the context window corresponds to one of the most active nodes in this layer. 

        **Summary**: As you watch generation, you'll see the network "fill up" row by row. The red dots in `Inputs` select embedding rows, colored activations flow through `Q`/`K`/`V` and attention (with triangular masking), the MLP adds knowledge (and bias for empty positions), and finally the `Predicted next token` layer shows what the model predicts should come next.

        ## What's Not Visualized

        Several important activation matrices are not directly shown but are indicated by the arrows showing information flow. The transformer block maintains a residual stream of activations that flows through the network, with each component adding information to this stream. The arrows and connection lines suggest this flow, but the actual activation values aren't displayed.

        The activation matrices that exist but aren't visualized include: the actual input to the transformer block (what comes from the `Embedding` layer), the intermediate results along the residual stream (after the attention mechanism adds its contribution, and again after the feed-forward network adds its contribution), and the logits (pre-softmax scores) that feed into the `Softmax sequence` layer. You can see the results of operations (like `q`, `k`, `v`, `FF Input`, `FF Hidden`, `FF Output`) but not the residual stream itself as it accumulates information.

        These activation matrices all share the same shape: `context_window × embedding_dim`. This consistent shape allows them to be added together in the residual connections, which is how information from earlier layers is preserved and combined with new information from attention and feed-forward processing. The `q`, `k`, and `v` visualizations show activation matrices produced by multiplying the residual stream by the `Q`, `K`, and `V` weight matrices respectively.

        ## Weight Matrices

        `Embedding`, `unembedding`, `K`, `Q`, `V`, `Input -> Hidden` and `Hidden -> Output` are weight matrices that don't change during inference, while clicking step or play, but that do change during training, as the system learns.
        
        ## Configuration
        
        When you first start this simulation, a dialog appears with these options:
        
        - `Context Size`: Number of tokens the model can see at once. Larger contexts allow the model to capture longer-range patterns but require more memory and training time.
        
        - `Embedding Dimension`: Size of the internal representation vectors. Higher dimensions allow more expressive representations but require more training data.
        
        - `Hidden Size`: Number of units in the transformer's feed-forward layer.
        
        - `Training Text`: The document used to train the model. The longer the document, the more patterns the model can learn, but training will take longer.
        
        - `Test Text`: Optional separate text file for validation. If not provided, the training text is automatically split.
        
        - `Sampling Strategy`: How the model chooses the next token from the probability distribution (see  below for more details).
        
        # What to Do
        
        ## Initial Setup
        
        When the simulation starts, you'll see a configuration dialog. Choose your settings or use the defaults. The simulation comes with several sample text files in the `simulations/texts/` directory.
        
        ## Training Your Model

        The model starts untrained. To train it:

        1. Click the `Supervised Model` interaction box in the network
        2. Open the training dialog (see the [training networks documentation](https://docs.simbrain.net/docs/network/trainingNetworks.html) for details)
        3. Click `Run` to train on your text corpus
        4. Watch the error plots to monitor learning progress
        5. Train until the loss curve appears to plateau (flatten out) - this typically indicates the model has learned as much as it can from the data

        Early stopping is disabled by default, so you control when training stops. You can always resume training later if the model hasn't converged enough. Training teaches the model to predict what token comes next based on context. The model learns patterns like common word sequences, grammar, and text structure.
        
        ### Details about the training process

        First, the input text is tokenized and split sequentially into training and test sets based on the train-test split ratio (specified in the configuration dialog). This preserves the sequential nature of the text rather than shuffling it. Then a sliding window of size `contextSize + 1` moves across the training text. Each window splits into an input and a target that is offset by one. For example, with `contextSize = 3` and text "hello there old friend!", the first window creates input `["hello", "there", "old"]` and target `["there", "old", "friend"]`. The second window (shifted by one position) creates input `["there", "old", "friend"]` and target `["old", "friend", "!"]`.

        This is a form of sequence-to-sequence prediction where all positions train simultaneously. In a single forward pass, position `0` learns to predict what comes after the first token, position `1` learns what comes after the second token, and so on. This is why the first row of the `Softmax sequence` layer predicts from minimal context: it represents what comes after just the first token. The targets are automatically generated by shifting inputs forward by one token.

        ## Using Your Trained Model

        Once trained, you can use the model to generate text:

        1. Type a prompt in the `Text Inputs` component (any text you want the model to continue)
        2. Click the `Play` button in the main toolbar
        3. Watch as the model generates new tokens, extending your prompt
        4. The model will continue generating until you press `Stop`

        The `Predicted next token` layer shows the probability distribution over possible next tokens. Higher activations indicate more likely continuations.

        ### Control Panel Features

        The Language Model Controls panel provides several tools for interacting with the trained model:

        - `Show Vocabulary`: Display all tokens in the model's vocabulary in a scrollable window. Each token is shown on its own line, with the total count displayed in the window title. This helps you understand exactly what tokens the model can recognize and generate.

        - `Show Training Text`: Display the original training text in a separate scrollable window. This is useful for viewing what the model was trained on, comparing model outputs to the original training data, and understanding the vocabulary and style the model learned.

        - `Temperature` slider: Adjust the randomness of generation. Lower values (`0.1-0.5`) make output more deterministic and focused, higher values (`1.0-2.0`) make output more creative and random.

        - `Configure Sampling Strategy`: Open a dialog to adjust how the model samples from the probability distribution (Greedy, Top-K, or Top-P sampling).

        ## Saving and Reopening
        
        After training, save your workspace to preserve the learned weights. When reopening a saved workspace, use the `Load workspace` button in the control panel below the text world to properly restore the update actions.
        
        ## Experiments
        
        ## Train on Different Text Types
        
        The default training text is `casual_texting_small.txt`, which was generated by AI to mimic short, casual human text exchanges. It contains conversational patterns like greetings, questions, and responses that help the model learn natural (albeit cringe!) dialogue flow.
        
        Try training on different text corpora to see how the model adapts. Each text type will produce different learned patterns. You can also create your own training data.
        
        ### Creating Training Data with AI
        
        You can generate custom training text using AI tools like ChatGPT or Claude. The key is to create coherent text with a limited vocabulary (around `50` unique tokens is a good starting point) that includes the patterns you want the model to learn.
        
        Example prompt for generating training data:
        
        "I am preparing training data for a small language model. Write a coherent, conversational dialogue about [your topic] that is suitable as training text. Constraints: Produce `25` sentences using EXACTLY `50` unique tokens (words + punctuation). Include multiple responses to the same question so the model can learn to generalize. Each sentence must be on its own line. Do NOT number the sentences. Do NOT include speaker labels. Make it sound like a conversation with questions, answers, and follow-ups. Use simple, clear English. Output ONLY the sentences."
        
        After generating the text, you can fine-tune it by hand to ensure it has the patterns and vocabulary you want the model to learn.
        
        ## Adjust Context Size
        
        Experiment with different context sizes:
        
        - Small contexts (`8-12` tokens): Faster training, captures local patterns
        - Large contexts (`24-48` tokens): Slower training, captures longer-range dependencies
        
        Try prompts that require different amounts of context to complete sensibly.
        
        ## Modify Sampling Strategies
        
        The sampling strategy determines how the model selects the next token. In the `Text Inputs` component, try different options:
        
        - `Greedy`: Always picks the most probable token. Produces deterministic, predictable output that may be repetitive.
        
        - `Top-K`: Randomly samples from the K most probable tokens. Good balance between creativity and coherence. Try K values from `3` to `10`.
        
        - `Top-P (Nucleus)`: Builds a "nucleus" by sorting the tokens by probability, selecting enough to reach `P` (build up cumulative probability to at least `P`), then samples from that nucleus proportionally. More dynamic than Top-K because the number of tokens varies based on the probability distribution. Try `P` values from `0.8` to `0.95`.
        
        ## Adjust Temperature
        
        Click on the `Softmax sequence` layer and adjust its temperature parameter or just use the slider bar in the control panel:
        
        - Low temperature (`0.1-0.5`): More confident, focused predictions (less random)
        - Medium temperature (`0.5-1.0`): Balanced randomness
        - High temperature (`1.0-2.0`): More diverse, creative, but potentially incoherent output
        
        Observe how temperature interacts with sampling strategy to affect generation quality.
        
        ## Regularization Experiments
        
        In the startup dialog's Regularization tab, try different settings:
        
        - `Weight Decay`: Prevents overfitting by penalizing large weights. Try values from `0.001` to `0.1`.
        - `Learning Rate Decay`: Gradually reduces the learning rate during training for more stable convergence.
        - `AdamW vs Adam`: Compare the AdamW optimizer (with decoupled weight decay) to standard Adam.
        
        Regularization is especially important with small training datasets where overfitting is common.
        
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
        softmaxSequence.highlightedRows = setOf(tokenPosition)
    }

    workspace.addUpdateAction("Predict Next Word") {
        val nextWord = textWorld.sampleToken(inferenceOutput.activationArray)
        // update text with predicted word and remove first word so that the context window maintains its size
        val newText = textWorldComponent.world.text.tokenize(textWorld.tokenizer)
            .map { it.token }
            .plus(nextWord)
            .takeLast(contextSize)
            .tokensToString(textWorld.tokenizer)
        // Use suspend versions to await UI updates and prevent backpressure
        textWorldComponent.world.setTextSuspend(newText)
        textWorldComponent.world.setCurrentTokenIndexSuspend(textWorldComponent.world.tokens.lastIndex)
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
