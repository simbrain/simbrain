package org.simbrain.custom_sims.simulations.nettalk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.AdamOptimizer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.Events
import org.simbrain.util.UserParameter
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.nettalk.loadNettalkCorpus
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.workspace.Workspace
import org.simbrain.world.speechsynthesizer.SpeechSynthesizer
import org.simbrain.world.speechsynthesizer.SpeechSynthesizerComponent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter

/**
 * NETtalk wired as a task-specific reader panel, a trainable network, and a reusable
 * speech synthesizer component.
 */
val nettalkComponentSim = newSim("nettalk_component") {

    workspace.clearWorkspace()

    val numWords = 1000
    val hiddenSize = 80
    val windowSize = 7

    val reader = NettalkReader(windowSize)

    val networkComponent = addNetworkComponent("Network")
    val net = networkComponent.network
    val encoder = NettalkEncoder(windowSize)

    val bp = BackpropNetwork(intArrayOf(encoder.inputSize, hiddenSize, encoder.outputSize)).apply {
        trainerConfig.optimizer = AdamOptimizer(beta2 = 0.9)
        trainerConfig.learningRate = 0.01
        trainerConfig.testConfiguration.enabled = false
    }
    net.addNetworkModels(bp)

    val corpus = loadNettalkCorpus(byFrequency = true)
        .filter { it.isRegular }
        .take(numWords)
    val tensors = encoder.encodeAsContinuousText(corpus, gap = 1, repeats = 3, shuffleSeed = 42L)
    bp.trainingSet = TrainingDataset(
        inputs = tensors.inputs.map { it.toMutableList() }.toMutableList(),
        targets = tensors.targets.map { it.toMutableList() }.toMutableList(),
        inputRowNames = tensors.rowLabels,
        targetRowNames = tensors.rowLabels,
        targetColumnNames = NettalkPhonology.featureNames + NettalkPhonology.stressNames
    )
    bp.testingSet = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = encoder.inputSize,
        targetSize = encoder.outputSize
    )
    bp.initBiases()
    bp.initWeights()
    bp.inputLayer.gridMode = true
    bp.hiddenLayers().firstOrNull()?.gridMode = true
    bp.outputLayer.gridMode = true
    bp.inputLayer.location = point(-298, 321)
    bp.hiddenLayers().firstOrNull()?.location = point(238, 13)
    bp.outputLayer.location = point(-275, -214)

    val speechComponent = addSpeechSynthesizer("Speech Synthesizer").apply {
        synthesizer.inputMode = SpeechSynthesizer.InputMode.ARTICULATORY_FEATURES
    }
    wireNetTalkCouplings(workspace, reader)
    workspace.addUpdateAction("Advance NETtalk reader") {
        reader.update()
    }

    withGui {
        val readerFrame = createControlPanel("NETtalk Reader", 0, 0) {
            centralPanel.removeAll()
            centralPanel.add(NettalkReaderPanel(reader), BorderLayout.CENTER)
        }
        readerFrame.setBounds(0, 0, 636, 256)
        place(networkComponent, 625, 0, 851, 835)
        place(speechComponent, 0, 241, 637, 593)
    }

    addSidebarInfo(NETTALK_COMPONENT_SIDEBAR)
}.registerReopenFunction { workspace ->
    val reader = NettalkReader()
    wireNetTalkCouplings(workspace, reader)
    workspace.addUpdateAction("Advance NETtalk reader") {
        reader.update()
    }
    withGui {
        createControlPanel("NETtalk Reader", 0, 0) {
            centralPanel.removeAll()
            centralPanel.add(NettalkReaderPanel(reader), BorderLayout.CENTER)
        }.setBounds(0, 0, 636, 256)
        workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull()?.let {
            place(it, 625, 0, 851, 835)
        }
        workspace.componentList.filterIsInstance<SpeechSynthesizerComponent>().firstOrNull()?.let {
            place(it, 0, 241, 637, 593)
        }
    }
    addSidebarInfo(NETTALK_COMPONENT_SIDEBAR)
}

private val NETTALK_COMPONENT_SIDEBAR: String = """
    # NETtalk

    NETtalk demonstrates how a neural network can learn to convert written English into
    phoneme-like speech output. A task-specific reader panel presents the text and centered
    letter window, while a separate `SpeechSynthesizerComponent` speaks from the network's
    26-dimensional articulatory feature output.

    # Coupling Topology

    - `NettalkReader.currentWindow` -> `Network.inputLayer.activationArray`
    - `Network.outputLayer.activationArray` -> `SpeechSynthesizer.speakFeatureVector`

    # What to Do

    1. Train the network: right-click the `Network` component -> `Edit/Train Backprop...`,
       then play. Mean error should drop steadily.
    2. Press the workspace play button. The reader advances through the text and the
       speech synthesizer decodes feature vectors to phonemes.
       Early in training the output often collapses to an "uh"-like vowel sound; this
       is normal before the network has learned useful letter-to-phoneme mappings.
    3. Edit the reading text directly in the NETtalk Reader panel to test other passages.

    # Links

    Simbrain's speech output is based on [eSpeak NG](https://github.com/espeak-ng/espeak-ng),
    a compact open-source text-to-speech system that supports text and phoneme input.

    # Credits

    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
""".trimIndent()

fun SimulationScope.wireNetTalkCouplings(workspace: Workspace, reader: NettalkReader) {
    val networkComp = workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull()
        ?: error("No NetworkComponent found in workspace.")
    val speechComp = workspace.componentList.filterIsInstance<SpeechSynthesizerComponent>().firstOrNull()
        ?: error("No SpeechSynthesizerComponent found in workspace.")
    val bp = networkComp.network.allModelsDeep.filterIsInstance<BackpropNetwork>().firstOrNull()
        ?: error("No BackpropNetwork found in the network component.")
    val synthesizer = speechComp.synthesizer
    synthesizer.inputMode = SpeechSynthesizer.InputMode.ARTICULATORY_FEATURES
    with(workspace.couplingManager) {
        reader.getProducer(reader::currentWindow) couple bp.inputLayer.getConsumer(bp.inputLayer::setActivations)
        bp.outputLayer.getProducer(bp.outputLayer::activationArray) couple synthesizer.getConsumer(synthesizer::speakFeatureVector)
    }
}

class NettalkReader(windowSize: Int = 7) : AttributeContainer {

    @UserParameter(label = "Auto advance", description = "If true, advance one character per workspace update.", order = 10)
    var autoAdvance: Boolean = true

    @UserParameter(label = "Window size", description = "Letter window width centered on the current letter.", order = 20, minimumValue = 1.0)
    var windowSize: Int = windowSize
        set(value) {
            require(value >= 1 && value % 2 == 1) { "windowSize must be a positive odd integer" }
            field = value
            encoder = NettalkEncoder(value)
        }

    @set:Consumable(description = "Replace the text being read.")
    var text: String = DEFAULT_TEXT
        set(value) {
            field = value
            position = if (value.isEmpty()) 0 else position.coerceIn(0, value.length - 1)
            events.textChanged.fire()
        }

    @set:Consumable(description = "Jump the reading position to the given character index.")
    var position: Int = 0
        set(value) {
            val clamped = if (text.isEmpty()) 0 else value.coerceIn(0, text.length - 1)
            if (field != clamped) {
                field = clamped
                events.positionChanged.fire()
            }
        }

    private var encoder = NettalkEncoder(windowSize)

    val events = NettalkReaderEvents()

    @get:Producible(description = "Current letter window encoded for a NETtalk network.")
    val currentWindow: DoubleArray
        get() = encoder.encodeWindow(text, position)

    @get:Producible(description = "The current letter at the reading position.")
    val currentLetter: String
        get() = text.getOrNull(position)?.toString() ?: ""

    fun reset() {
        position = 0
        events.positionChanged.fire()
    }

    fun update() {
        if (text.isEmpty()) return
        if (autoAdvance) {
            position = (position + 1) % text.length
        }
    }

    override val id: String = "NETtalk Reader"

    companion object {
        const val DEFAULT_TEXT: String =
            "the quick brown fox jumps over the lazy dog. " +
                "she sells sea shells by the sea shore. " +
                "peter piper picked a peck of pickled peppers."
    }
}

class NettalkReaderEvents : Events() {
    val textChanged = NoArgEvent()
    val positionChanged = NoArgEvent()
}

private class NettalkReaderPanel(private val reader: NettalkReader) : JPanel() {

    private val textArea = JTextArea(reader.text).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        rows = 6
        columns = 60
    }

    private val cursorPainter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 235, 100))

    private val windowLabel = JLabel(" ").apply {
        font = Font(Font.MONOSPACED, Font.BOLD, 22)
    }

    private val textLabel = JLabel("Text to read").apply {
        font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        foreground = Color(80, 80, 80)
    }

    private val windowSectionLabel = JLabel("Centered letter window").apply {
        font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        foreground = Color(80, 80, 80)
    }

    init {
        preferredSize = Dimension(616, 205)
        layout = MigLayout("fill, wrap 1, ins 10, gapy 4", "[grow]", "[][grow][][pref][]")
        add(textLabel, "growx")
        add(JScrollPane(textArea), "grow, hmin 92")
        add(windowSectionLabel, "growx, gapy 4 0")
        add(windowLabel, "alignx center, gapy 0 2")
        add(JButton("Reset").apply {
            toolTipText = "Reset reading position to 0"
            addActionListener { reader.reset() }
        }, "alignx right")

        textArea.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = syncFromArea()
            override fun insertUpdate(e: DocumentEvent) = syncFromArea()
            override fun removeUpdate(e: DocumentEvent) = syncFromArea()
        })

        reader.events.textChanged.on(Dispatchers.Swing) {
            if (textArea.text != reader.text) textArea.text = reader.text
            updateHighlights()
            updateReadout()
        }
        reader.events.positionChanged.on(Dispatchers.Swing) {
            updateHighlights()
            updateReadout()
        }
        updateHighlights()
        updateReadout()
    }

    private fun syncFromArea() {
        if (textArea.text != reader.text) {
            reader.text = textArea.text
        }
    }

    private fun displayPosition(): Int {
        return reader.position
    }

    private fun updateHighlights() {
        try {
            val areaHighlighter = textArea.highlighter
            areaHighlighter.removeAllHighlights()
            val len = reader.text.length
            if (len > 0) {
                val pos = displayPosition().coerceIn(0, len - 1)
                areaHighlighter.addHighlight(pos, (pos + 1).coerceAtMost(len), cursorPainter)
            }
        } catch (_: Exception) {
        }
    }

    private fun updateReadout() {
        val windowSize = reader.windowSize
        val pad = "_".repeat(windowSize / 2)
        val padded = pad + reader.text.lowercase() + pad
        val pos = displayPosition()
        val window = if (padded.length >= pos + windowSize) {
            padded.substring(pos, pos + windowSize)
        } else {
            padded.padEnd(pos + windowSize, '_').substring(pos, pos + windowSize)
        }
        val center = windowSize / 2
        windowLabel.text = buildString {
            append("<html><span style='font-family:monospace'>")
            for ((i, c) in window.withIndex()) {
                if (i == center) {
                    append("<span style='background-color:#ffe764'>&nbsp;${escapeHtml(c)}&nbsp;</span>")
                } else {
                    append("&nbsp;${escapeHtml(c)}&nbsp;")
                }
            }
            append("</span></html>")
        }

        updateHighlights()
    }

    private fun escapeHtml(c: Char): String = when (c) {
        '<' -> "&lt;"
        '>' -> "&gt;"
        '&' -> "&amp;"
        ' ' -> "&middot;"
        else -> c.toString()
    }

}
