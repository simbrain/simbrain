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
        description = "Maximum number of tokens the model can retain (up to 32,000). " +
            "Larger windows use substantially more memory; 512 is recommended for most computers.",
        min = 1,
        max = 32_000,
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
val lfm2LanguageModel = newSim {

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

    val textWorldComponent = addTextWorld("Document")
    textWorldComponent.world.autoAdvance = false
    textWorldComponent.world.showTokenBoundaries = false
    textWorldComponent.world.text = "Here is a brief two-paragraph parable:"

    setupLfm2DocumentSync(workspace)

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
                "Tools", toolOptions, toolOptions.first(),
                labelToolTip = "Choose tools to include when a new chat context begins; press Reset before sending",
            ) { selection ->
                languageModel.demoToolsEnabled = selection == "Current time"
            }.apply {
                toolTipText = "Choose tools to include when a new chat context begins; press Reset before sending"
            }

            addComboBox(
                "Color theme", DocumentStructureDisplay.entries, DocumentStructureDisplay.CONVERSATION_FOCUS,
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

    addSidebarInfo(
        """
        # LFM2.5-230M in Simbrain

        A real, full-scale language model — [LFM2.5-230M](https://huggingface.co/LiquidAI/LFM2.5-230M) by Liquid AI — running entirely inside Simbrain. Nothing is mocked and no network calls are made during generation: every matrix multiply happens in this process, and the diagram on the network canvas is the model's actual interior, repainted as it computes.

        The weights (~460 MB) are downloaded from Hugging Face on first use and cached locally. They are licensed under the [LFM Open License v1.0](https://www.liquid.ai/lfm-license).

        # Completion (Default)

        Completion is what happens when you press the desktop `Play` button with the default `Completion` prompt mode. The model continues the `Document` verbatim: no chat markers are added. Every workspace step is one forward pass: the model first reads the document one token per step (prompt processing), then generates one sampled token per step until it emits `<|im_end|>`, fills the context window, or reaches the optional token limit. Simbrain pauses and unlocks the document.

        The `<|im_end|>` marker seals the stream: as long as the document ends with it, the model considers the text finished. Delete the marker (or edit the text anywhere) to continue. After an edit the model re-reads only from near the change — the unchanged beginning of the document is still in its caches, the way real inference servers reuse a prompt prefix. There is no prompt hiding anywhere — the document IS the model's entire input. `Reset` in the control panel clears both the document and the model's context, and the temperature and sampling strategy can change mid-run.

        # Chat

        Select `Chat` in `Settings`, then press `Reset` to start a fresh conversation. Type into `Chat message` and press Enter (or `Send message`). Simbrain adds `<|startoftext|>`, a complete `<|im_start|>user` turn, and an open `<|im_start|>assistant` turn, then starts `Play` automatically. The reply runs until it emits `<|im_end|>`, which closes the assistant turn; Simbrain pauses and unlocks the document for the next message.

        You can switch between `Completion` and `Chat` in `Settings` at any time and inspect or edit the `Document` to see the exact token stream. Completion leaves the document alone; chat adds its turn markers only when you send a message.

        # Reading the Diagram

        Data flows bottom to top through the residual stream — the column of wide tiles. LFM2 is a hybrid: most blocks are short-convolution blocks, with a few attention blocks in between. Use the depth strip to select which layer's interior is shown, scroll over the attention deck to flip through heads, and watch the logit-lens readouts sharpen as you read up the stream.

        Hover over any cell to read its value; double-click a tile to trace its data-flow paths. The limb the selected layer doesn't use stays faintly visible for orientation; right-click the model and set `Inactive limb` to `Hide` to clear it away entirely.

        Most of what the tall tiles show is a *recording*: at each step the model only holds the current token's activations, and the diagram keeps the old rows so you can see the trajectory. Right-click the model and set `Token history` to `Ghost` to see what is genuinely resident — past rows ghost out, one bright row sweeps down as it writes, and the only tiles left fully lit are the KV caches and the conv window. That is the model's entire memory, and it is why those caches exist. `Off` goes further and keeps no history at all — layer flips become instant, and switching back re-derives the recording from the depth strip.

        # Special Tokens

        Bold neutral text in the `Document` is a special token: a single token with a control meaning rather than ordinary language. LFM2 uses a ChatML-like format in which `<|im_start|>` and `<|im_end|>` mean _start of the message_ and _end of the message_; Liquid AI does not expand `im` further. `Conversation focus` is the default color theme; it fades the harness so the conversation stands out. `Role colors` renders user text blue, assistant text green, system text violet, and tool activity teal.

        - `<|startoftext|>` begins every document. Simbrain adds it automatically if you omit it.
        - `<|im_start|>user` opens a user turn; `<|im_start|>assistant` opens the reply the model is expected to write.
        - `<|im_end|>` closes a turn and normally seals generation.
        - `<|tool_call_start|>` / `<|tool_call_end|>` surround a model tool request.
        - `<|tool_response_start|>` / `<|tool_response_end|>` surround a tool result.

        The role names (`user`, `assistant`, `system`, and `tool`) are ordinary text immediately after `<|im_start|>`; they are bold too because they are part of the harness, not text normally shown in a chat interface. You can edit any marker to see how changing the harness changes the model's behavior. LFM2 has additional special tokens for other tasks; see the [full tokenizer list](https://huggingface.co/LiquidAI/LFM2-Tokenizer#special-tokens).

        # Tools

        Select `Current time` from `Tools`, then start a fresh conversation with `Reset`. The model is then told about the local `current_time` tool. Ask “What time is it?” The model decides whether to emit a tool call; when it does, Simbrain reads this computer's clock, adds a `tool` result turn, and lets the model continue its reply.

        # Beta

        This simulation is a beta: it exists partly to exercise the language-model UX. If a status line confuses you, a control feels missing, or an edit does something surprising, that is exactly the feedback it is for.
        """.trimIndent()
    )
}.registerReopenFunction { workspace ->
    setupLfm2DocumentSync(workspace)
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
            !languageModel.isLoaded -> null
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
