package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.Dispatchers
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.PromptMode
import org.simbrain.network.llm.obtainWeightsInteractive
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.DocumentStructureDisplay
import org.simbrain.world.textworld.TextWorldComponent

class Lfm2LanguageModelOptions : EditableObject {

    var contextWindowSize by GuiEditable(
        initValue = 512,
        label = "Context window size",
        description = "Maximum number of tokens the model can retain (up to 8,192). " +
            "Larger windows use substantially more memory; 512 is recommended for most computers.",
        min = 1,
        max = 8192,
        order = 1,
    )

    override val name = "LFM2 Language Model"
}

/**
 * Showcase for the full-scale language model: LFM2.5-230M running entirely inside Simbrain,
 * with its interior rendered live on the network canvas and its context window synced to a
 * text world document. The weights (~460 MB) are downloaded from Hugging Face on first use;
 * Simbrain never bundles them.
 */
val lfm2LanguageModel = newSim("lfm2_language_model") {

    val options = Lfm2LanguageModelOptions().showAPEOptionDialog("LFM2 Language Model")
        ?: return@newSim

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val languageModel = LanguageModel("", options.contextWindowSize).apply {
        label = "LFM2.5-230M"
    }
    withGui { languageModel.obtainWeightsInteractive(network) }
    with(network) {
        addNetworkModels(languageModel)
    }
    languageModel.location = point(0, 0)

    val textWorldComponent = addTextWorld("Context Window")
    textWorldComponent.world.autoAdvance = false
    textWorldComponent.world.showTokenBoundaries = false
    textWorldComponent.world.text = "Here is a brief two-paragraph parable:"

    setupLfm2DocumentSync(workspace)
    setupLfm2Gui(workspace)

    withGui {
        val textWorldDesktopComponent = SimbrainDesktop.getDesktopComponent(textWorldComponent)
        SimbrainDesktop.onboardingManager.showPopup(
            PopupConfig(
                title = "The Model's Document",
                message = "This document is the model's context window. Press Play on the main " +
                    "toolbar to watch the model read it one token per step, then generate its " +
                    "continuation the same way — the workspace pauses itself when the model " +
                    "finishes. Edit the text and press Play again to continue from your edit.",
                targetComponent = textWorldDesktopComponent as javax.swing.JComponent,
                suppressionKey = "lfm2_language_model_document_help",
                placement = PopupPlacement.BOTTOM_CENTER,
                style = PopupStyle.SUCCESS,
            )
        )
    }
}.registerReopenFunction { workspace ->
    setupLfm2DocumentSync(workspace)
    setupLfm2Gui(workspace)
}

/** The sim's desktop chrome — control panel, window placement, sidebar — rebuilt on reopen too. */
private suspend fun SimulationScope.setupLfm2Gui(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val languageModel = networkComponent.network.getModels<LanguageModel>().first()
    val textWorldComponent = workspace.componentList.filterIsInstance<TextWorldComponent>().first()

    withGui {
        val textWorldWidth = 420
        val textWorldHeight = 380
        val controlPanel = createControlPanel(
            "Language Model Controls",
            SIM_WINDOW_GAP,
            SIM_WINDOW_GAP + textWorldHeight + SIM_WINDOW_GAP,
        ) {
            addSliderWithTextField("Temperature", 0.01, 4.0, languageModel.temperature, 0.01) { temp ->
                languageModel.temperature = temp
            }

            addButton("Settings...") {
                languageModel.createEditorDialog("Language Model Settings").display()
            }.apply {
                toolTipText = "Change prompt mode, token limit, sampling strategy, and other generation settings"
            }

            addButton("Reset") {
                textWorldComponent.world.text = ""
                languageModel.clearWindow()
            }.apply {
                toolTipText = "Clear the document and start a new model context"
            }

            addSeparator()

            val toolOptions = listOf("None", "Current time")
            addComboBox(
                "Tools", toolOptions, if (languageModel.demoToolsEnabled) "Current time" else "None",
                labelToolTip = "Choose tools to include when a new chat context begins; press Reset before sending",
            ) { selection ->
                languageModel.demoToolsEnabled = selection == "Current time"
            }.apply {
                toolTipText = "Choose tools to include when a new chat context begins; press Reset before sending"
            }

            addComboBox(
                "Color theme", DocumentStructureDisplay.entries, textWorldComponent.world.documentStructureDisplay,
                labelToolTip = "Choose how chat roles, system text, and tool calls appear in the document",
            ) { display ->
                textWorldComponent.world.documentStructureDisplay = display
            }.apply {
                toolTipText = "Choose how chat roles, system text, and tool calls appear in the document"
            }

            addSeparator()

            val messageField = addTextField(
                "Chat message", "",
                labelToolTip = "In Chat prompt mode, add a user turn and generate an assistant reply",
            ).apply {
                toolTipText = "In Chat prompt mode, add a user turn and generate an assistant reply"
            }
            fun sendChatMessage() {
                if (!languageModel.isLoaded) return
                val message = messageField.text.trim()
                if (message.isEmpty()) return
                languageModel.sendUserMessage(message)
                messageField.text = ""
                if (!workspace.updater.isRunning) workspace.run()
            }
            messageField.addActionListener { sendChatMessage() }
            addButton("Send message") {
                sendChatMessage()
            }
        }.awaitLayout()
        controlPanel.setLocation(
            controlPanel.centeredXInColumn(SIM_WINDOW_GAP, textWorldWidth),
            SIM_WINDOW_GAP + textWorldHeight + SIM_WINDOW_GAP,
        )
        place(textWorldComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, textWorldWidth, textWorldHeight)
        place(networkComponent, SIM_WINDOW_GAP + textWorldWidth + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 1000, 760)
    }

    addSidebarInfo(
        """
        # LFM2.5-230M in Simbrain

        A full-scale language model running entirely inside Simbrain. The model is [LFM2.5-230M](https://huggingface.co/LiquidAI/LFM2.5-230M) (LFM stands for liquid foundation model) from Liquid AI. The weights (~460 MB) are downloaded from Hugging Face on first use and cached locally. They are licensed under the [LFM Open License v1.0](https://www.liquid.ai/lfm-license).

        The model uses a combination of convolutions to reduce the size of the input stream and standard [attention](https://huggingface.co/docs/transformers/main/attention) heads, along with caching and optimization strategies such as the [KV cache](https://huggingface.co/docs/transformers/en/kv_cache) and query-based attention.

        A new set of graphical tools was developed to display such a complex network. Several modes are possible for displaying the network, accessible via the context window. A central graphical element is the layer nodes on the left side. Each box corresponds to one layer and ask you click on these layers the full set of components in that layer is revealed in the main window. The final layer shows which tokens in the vocabulary are most probable. The top element is often the one added to the context window in the next iteration.

        Information flows from bottom to top and the activations of the layer nodes fill up row by row as the context window is processed. Each token corresponds to one row. You will see many elements slow fill up row by row as context is used.

        All tokens, including those that are usually hidden in a standard AI chat, are visible. A color theme can be selected which can be used to make the status of different tokens clear. The bottom of the context window contains important information about how tokens are being processed.

        This simulation is marked as beta: it is a complex simulation, and our sense of how best to present the information is evolving. Your feedback is welcome!

        # Control Panel

        - `Temperature` controls how varied the generated tokens are.
        - `Settings...` changes the prompt mode, token limit, sampling strategy, and other generation settings.
        - `Reset` clears the context window and starts a new model context.
        - `Tools` enables the optional `Current time` tool for a new chat context.
        - `Color theme` changes how chat roles, system text, and tool calls appear.
        - In `Chat` prompt mode, enter text in `Chat message` and use `Send message` (or press Enter) to add a user turn and generate a reply.

        # How to interact with the network

        The simplest way to interact with the network is by simply entering text in the context window and pressing play. The network will run until it hits `<|im_end|>`. Initially it processes the existing prompt then it switches to generating new tokens, as indicated by the status bar.

        You can also enter a chat message, which adorns the message with appropriate tokens so you can have a familiar chat style interaction without having to manually encode those.

        # Reading the Diagram

        Data flows bottom to top through the residual stream. LFM2 is a hybrid: most blocks are short-convolution blocks, with a few attention blocks in between. Use the depth strip to select which layer's interior is shown, scroll over the attention deck to flip through heads, and watch the logit-lens readouts sharpen as you read up the stream.

        Hover over any cell to read its value; double-click a tile to trace its data-flow paths. The limb the selected layer doesn't use stays faintly visible for orientation; right-click the model and set `Inactive limb` to `Hide` to clear it away entirely.

        Most of what the tall tiles show is a recording: at each step the model only holds the current token's activations, and the diagram keeps the old rows so you can see the trajectory. Right-click the model and set `Token history` to `Ghost` to see what is genuinely resident: past rows ghost out, one bright row sweeps down as it writes, and the only tiles left fully lit are the KV caches and the conv window. That is the model's entire memory, and it is why those caches exist. `Off` goes further and keeps no history at all. Layer flips become instant, and switching back re-derives the recording from the depth strip.

        # Special Tokens

        Neutral text in the `Context Window` can be a special token: a single token with a control meaning rather than ordinary language. LFM2 uses a ChatML-like format in which `<|im_start|>` and `<|im_end|>` mean _start of the message_ and _end of the message_; Liquid AI does not expand `im` further. `Conversation focus` is the default color theme; it fades the harness so the conversation stands out. `Role colors` renders user text blue, assistant text green, system text violet, and tool activity teal.

        - `<|startoftext|>` begins every document. Simbrain adds it automatically if you omit it.
        - `<|im_start|>user` opens a user turn; `<|im_start|>assistant` opens the reply the model is expected to write.
        - `<|im_end|>` closes a turn and normally seals generation.
        - `<|tool_call_start|>` / `<|tool_call_end|>` surround a model tool request.
        - `<|tool_response_start|>` / `<|tool_response_end|>` surround a tool result.

        The role names (`user`, `assistant`, `system`, and `tool`) are ordinary text immediately after `<|im_start|>` and are part of the harness, not text normally shown in a chat interface. You can edit any marker to see how changing the harness changes the model's behavior. LFM2 has additional special tokens for other tasks; see the [full tokenizer list](https://huggingface.co/LiquidAI/LFM2-Tokenizer#special-tokens).

        # Tools

        Select `Current time` from `Tools`, then start a fresh conversation with `Reset`. The model is then told about the local `current_time` tool. Ask “What time is it?” The model decides whether to emit a tool call; when it does, Simbrain reads this computer's clock, adds a `tool` result turn, and lets the model continue its reply.


        """.trimIndent()
    )
}

/**
 * Wires the model's context window to the text world as a two-way document sync. The text world
 * adopts the model's tokenizer for token boxes and locks itself while the workspace runs; both
 * arrive through the coupling-adoption listener. The adopted tokenizer snapshots the weights
 * path, so when weights arrive later (first-run download) the world re-adopts on load.
 * Recreating existing couplings on reopen is safe: the coupling manager stores them in a set.
 */
private fun setupLfm2DocumentSync(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val languageModel = network.getModels<LanguageModel>().first()
    val textWorld = workspace.componentList.filterIsInstance<TextWorldComponent>().first().world
    textWorld.documentStructureDisplay = DocumentStructureDisplay.CONVERSATION_FOCUS
    textWorld.showTokenBoundaries = false
    textWorld.tokenCountLabelProvider = {
        val used = textWorld.tokens.size
        "$used used / ${(languageModel.maxSeqLen - used).coerceAtLeast(0)} remaining"
    }
    textWorld.statusMessageProvider = {
        when {
            !languageModel.isLoaded ->
                "Weights not loaded — right-click the model in the Network window to locate or download them"
            languageModel.isPromptProcessing ->
                "Processing prompt — token ${languageModel.fedTokenCount} of ${languageModel.windowTokenCount}"
            languageModel.isSealed -> if (languageModel.promptMode == PromptMode.CHAT)
                "Reply finished — send the next message"
            else "Finished — edit the text to continue"
            languageModel.isWindowFull -> "Context window full — Reset to start over"
            languageModel.budgetSpent -> "Token limit reached — edit the text to continue"
            languageModel.canAdvance -> "Generating — ${languageModel.generatedCount} tokens so far"
            else -> null
        }
    }

    with(workspace.couplingManager) {
        createCoupling(
            textWorld.getProducer("getText"),
            languageModel.getConsumer("setContextWindow"),
        )
        createCoupling(
            languageModel.getProducer("getContextWindow"),
            textWorld.getConsumer("setTextIfChanged"),
        )
        createCoupling(
            languageModel.getProducer("getCurrentTokenSpan"),
            textWorld.getConsumer("setHighlightSpan"),
        )
    }
    languageModel.events.weightsLoaded.on(Dispatchers.Default) {
        textWorld.displayTokenizer = languageModel.displayTokenizer.copy() as Tokenizer<*>
    }
}
