package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.Dispatchers
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.PromptMode
import org.simbrain.network.llm.obtainWeightsInteractive
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.TextWorldComponent

class Lfm2LanguageModelOptions : EditableObject {

    var prompt by GuiEditable(
        initValue = "The capital of France is",
        description = "Text the model starts from. In chat mode this is the user's first message.",
        order = 10,
    )

    var promptMode: PromptMode by GuiEditable(
        initValue = PromptMode.COMPLETION,
        label = "Prompt mode",
        description = "Completion continues the prompt verbatim; " +
            "chat wraps it as a user message the model answers",
        order = 20,
    )

    var systemPrompt by GuiEditable(
        initValue = "",
        label = "System prompt",
        description = "Optional system message ahead of the user message; chat mode only",
        order = 30,
    )

    var enableDemoTools by GuiEditable(
        initValue = false,
        label = "Enable demo tools",
        description = "Advertise the built-in offline demo tools (current time, canned weather) " +
            "in chat mode; ask about the weather to see a tool call happen",
        order = 40,
    )

    var maxSequenceLength by GuiEditable(
        initValue = 512,
        label = "Max sequence length",
        description = "Context window capacity; generation stops when it fills",
        order = 50,
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

    val languageModel = LanguageModel("", options.maxSequenceLength).apply {
        label = "LFM2.5-230M"
        prompt = options.prompt
        promptMode = options.promptMode
        systemPrompt = options.systemPrompt
        enableDemoTools = options.enableDemoTools
        samplingStrategy = SamplingStrategy.TopP()
    }
    withGui { languageModel.obtainWeightsInteractive(network) }
    with(network) {
        addNetworkModels(languageModel)
    }
    languageModel.location = point(0, 0)

    val textWorldComponent = addTextWorld("Document")
    textWorldComponent.world.highlightCurrentToken = false
    textWorldComponent.world.autoAdvance = false

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

            addButton("Configure sampling strategy...") {
                val wrapper = objectWrapper(
                    "Sampling Strategy",
                    languageModel.samplingStrategy.copy() as SamplingStrategy,
                )
                val editor = AnnotatedPropertyEditor(wrapper)
                StandardDialog(editor).apply {
                    title = "Configure Sampling Strategy"
                    addCommitTask {
                        editor.commitChanges()
                        languageModel.samplingStrategy = wrapper.editingObject as SamplingStrategy
                    }
                }.display()
            }

            addButton("Generation settings...") {
                languageModel.createEditorDialog("Language Model Settings").display()
            }

            addSeparator()

            addButton("Reseed context from prompt") {
                languageModel.seedFromPrompt()
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
                    "toolbar and watch it write, one token per step — the workspace pauses by " +
                    "itself when the model finishes. Edit the text and press Play again to " +
                    "continue from your edit.",
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

        # Generate Text

        1. Press `Play` (or `Step`) in the main toolbar. Each workspace step runs the full model once and samples one token.
        2. The `Document` window shows the model's context window — the prompt plus everything it has written. While the workspace runs, the document is read-only.
        3. When the model finishes — it writes its end-of-text marker, the visible `<|im_end|>` box — the workspace pauses itself and the document unlocks.
        4. Edit the document and press `Play` again. Your edit replaces the model's context and generation continues from it. You can also pause the workspace yourself at any time to edit mid-run.

        The `<|im_end|>` marker seals the stream: as long as the document ends with it, the model considers the text finished. Delete the marker (or edit the text anywhere) to continue. Use the control panel (or the model's right-click menu) to reseed the document from the prompt, and to change the temperature or sampling strategy mid-run.

        # Reading the Diagram

        Data flows top to bottom through the residual stream — the column of wide tiles. LFM2 is a hybrid: most blocks are short-convolution blocks, with a few attention blocks in between. Use the depth strip to select which layer's interior is shown, scroll over the attention deck to flip through heads, and watch the logit-lens readouts sharpen as you read down the stream.

        Hover over any cell to read its value; double-click a tile to trace its data-flow paths.

        # Chat and Tools

        Restart the simulation and choose `Chat` prompt mode to have the model answer your prompt as an assistant instead of continuing it verbatim. With `Enable demo tools` checked, asking something like "What's the weather in Paris?" makes the model emit a tool call, which Simbrain answers with demo data, and the model folds the result into its reply.

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

    with(workspace.couplingManager) {
        createCoupling(
            textWorld.getProducer("getText"),
            languageModel.getConsumer("setContextWindow"),
        )
        createCoupling(
            languageModel.getProducer("getContextWindow"),
            textWorld.getConsumer("setTextIfChanged"),
        )
    }
    languageModel.events.weightsLoaded.on(Dispatchers.Default) {
        textWorld.displayTokenizer = languageModel.displayTokenizer.copy() as Tokenizer<*>
    }
}
