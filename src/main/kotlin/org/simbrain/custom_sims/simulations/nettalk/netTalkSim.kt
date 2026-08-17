package org.simbrain.custom_sims.simulations.nettalk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.AdamOptimizer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.*
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.nettalk.loadNettalkCorpus
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.workspace.Workspace
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

    val speechComponent = addSpeechSynthesizer("Speech Synthesizer")
    wireNetTalk(workspace, reader)

    withGui {
        val readerFrame = createControlPanel("NETtalk Reader", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            centralPanel.removeAll()
            centralPanel.add(NettalkReaderPanel(reader), BorderLayout.CENTER)
        }
        readerFrame.setBounds(SIM_WINDOW_GAP, SIM_WINDOW_GAP, 636, 256)
        place(networkComponent, readerFrame.rightEdgeWithGap(), SIM_WINDOW_GAP, 851, 835)
        place(speechComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP + 256 + SIM_WINDOW_GAP, 637, 593)
    }

    addSidebarInfo(NETTALK_COMPONENT_SIDEBAR)
}.registerReopenFunction { workspace ->
    val reader = NettalkReader()
    wireNetTalk(workspace, reader)
    withGui {
        val readerFrame = createControlPanel("NETtalk Reader", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            centralPanel.removeAll()
            centralPanel.add(NettalkReaderPanel(reader), BorderLayout.CENTER)
        }
        readerFrame.setBounds(SIM_WINDOW_GAP, SIM_WINDOW_GAP, 636, 256)
        workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull()?.let {
            place(it, readerFrame.rightEdgeWithGap(), SIM_WINDOW_GAP, 851, 835)
        }
        workspace.componentList.filterIsInstance<SpeechSynthesizerComponent>().firstOrNull()?.let {
            place(it, SIM_WINDOW_GAP, SIM_WINDOW_GAP + 256 + SIM_WINDOW_GAP, 637, 593)
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
    
    Note: problems with this sim have been reported on some machines, which should be fixed in a release soon.

    # Simulation Details

    ## Inputs and Outputs

    Each network input is a sliding window over the text. With the default window size of
    7, the network sees the current character plus three characters of context on each
    side. Each position in the window is encoded as a 29-unit one-hot vector: 26 units for
    `a` through `z`, one blank/word-boundary unit, one end-of-sentence punctuation unit,
    and one general punctuation unit. The default input layer therefore has
    `7 x 29 = 203` units.

    Each network output is a 26-dimensional NETtalk speech code. The first 21 values are
    articulatory features such as place, manner, voicing, vowel height/backness, tenseness,
    and silence. The last 5 values encode stress or syllable markers. The speech
    synthesizer decodes these outputs by finding the nearest known phoneme feature pattern
    and then sending the resulting phoneme sequence to the speech synthesizer.

    Training examples are built from a continuous text stream with spaces between words,
    so the network also learns that word-boundary positions should map to silent output.

    ## Relation to the Original NETtalk

    This simulation is faithful to the classic NETtalk setup in its broad architecture:
    a 7-character input window, 29 one-hot units per character position, 80 hidden units,
    and 26 speech-code output units. It also uses the UCI NETtalk corpus, an updated and
    corrected version of the dataset associated with Sejnowski and Rosenberg's work.

    Simbrain does not exactly reproduce the original training protocol. The simulation uses
    Simbrain's modern backpropagation trainer with Adam, reorders the dictionary by word
    frequency, filters to regular words, repeats and shuffles the examples, and trains from
    a continuous stream built from those dictionary entries. This makes the demo easier to
    train and interact with, but it is not the same as either the paper's continuous informal
    speech corpus experiment or its exception-heavy dictionary experiment.

    The output representation is also a practical reconstruction. It preserves the 26-unit
    NETtalk-style target size and maps network outputs to phonemes through articulatory and
    stress features, but the feature names and decoding are adapted for Simbrain's current
    speech synthesizer rather than being a byte-for-byte recreation of the original DECtalk
    pipeline.

    ## Wiring

    - Update action `Set NETtalk inputs` copies `NettalkReader.currentWindow`
      into `Network.inputLayer` before each step.
    - Coupling: `Network.outputLayer.activationArray` -> `SpeechSynthesizer.speakFeatureVector`.
    - Update action `Flush NETtalk word at boundary` calls
      `SpeechSynthesizer.flushFeatureBuffer()` when the reader passes a non-letter
      character, so the synthesizer speaks one word at a time (in BUFFERED mode).

    # What to Do

    1. Train the network: right-click the `Network` component -> `Edit/Train Backprop...`,
       then play. The optimizer settings have been chosen because they work well for this
       demo. Training still takes some time: on many computers, around 200 iterations is a
       reasonable target and may take roughly 5-10 minutes. Mean error should drop steadily.
    2. Press `Run`. The reader advances through the text and the
       speech synthesizer decodes feature vectors to phonemes.
       Early in training the output often collapses to an "uh"-like vowel sound; this
       is normal before the network has learned useful letter-to-phoneme mappings.
    3. Edit the reading text directly in the NETtalk Reader panel to test other passages.

    # References

    Sejnowski, T. J., & Rosenberg, C. R. (1987). [_Parallel networks that learn to pronounce English text_](https://papers.cnl.salk.edu/PDFs/NETtalk_%20A%20Parallel%20Network%20That%20Learns%20to%20Read%20Aloud%201988-3562.pdf). _Complex Systems_, _1_, 145-168.

    # Links

    - [UCI Connectionist Bench NETtalk Corpus](https://archive.ics.uci.edu/dataset/150/connectionist%2Bbench%2Bnettalk%2Bcorpus)
    Simbrain's speech output is based on [eSpeak NG](https://github.com/espeak-ng/espeak-ng),
    a compact open-source text-to-speech system that supports text and phoneme input.

    # Credits

    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
""".trimIndent()

fun SimulationScope.wireNetTalk(workspace: Workspace, reader: NettalkReader) {
    val networkComp = workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull()
        ?: error("No NetworkComponent found in workspace.")
    val speechComp = workspace.componentList.filterIsInstance<SpeechSynthesizerComponent>().firstOrNull()
        ?: error("No SpeechSynthesizerComponent found in workspace.")
    val bp = networkComp.network.allModelsDeep.filterIsInstance<BackpropNetwork>().firstOrNull()
        ?: error("No BackpropNetwork found in the network component.")
    val synthesizer = speechComp.synthesizer
    workspace.addUpdateAction("Set NETtalk inputs", position = 0) {
        bp.inputLayer.setActivations(reader.currentWindow)
    }
    workspace.addUpdateAction("Flush NETtalk word at boundary") {
        val ch = reader.currentLetter.firstOrNull()
        if (ch != null && !ch.isLetter()) {
            synthesizer.flushFeatureBuffer()
        }
    }
    workspace.addUpdateAction("Advance NETtalk reader") {
        reader.update()
    }
    with(workspace.couplingManager) {
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

class NettalkReaderEvents : FlowEvents() {
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
        font = Theme.section
        foreground = Theme.mutedText
    }

    private val windowSectionLabel = JLabel("Centered letter window").apply {
        font = Theme.section
        foreground = Theme.mutedText
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
