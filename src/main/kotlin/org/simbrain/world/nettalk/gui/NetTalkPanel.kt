package org.simbrain.world.nettalk.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.util.DetailTrianglePanel
import org.simbrain.util.createApplyPanel
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.nettalk.NetTalk
import org.simbrain.world.soundworld.warnIfEspeakUnavailable
import java.awt.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter

/**
 * UI for [NetTalk]. Shows the reading text with cursor + currently-spoken-word highlight,
 * the active letter window, the predicted phoneme, a 26-bar articulatory feature display,
 * a running phonetic transcription, and controls for audio mode and reset.
 */
class NetTalkPanel(val nettalk: NetTalk) : JPanel() {

    private val synthEditor = AnnotatedPropertyEditor(nettalk.synthesizer)

    private val textArea = JTextArea(nettalk.text).apply {
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
    }

    private val cursorPainter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 235, 100))
    private val speakingPainter = DefaultHighlighter.DefaultHighlightPainter(Color(150, 230, 150))

    private val windowLabel = JLabel(" ").apply {
        font = Font(Font.MONOSPACED, Font.BOLD, 22)
    }
    private val phonemeReadout = JLabel(" ").apply {
        font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
    }
    private val featureBars = FeatureBarsPanel()
    private val transcriptionArea = JTextArea(2, 60).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val transcriptionScroll = JScrollPane(transcriptionArea).apply {
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }

    private val modeBox = JComboBox(NetTalk.AudioMode.values()).apply {
        selectedItem = nettalk.audioMode
        addActionListener {
            (selectedItem as? NetTalk.AudioMode)?.let { nettalk.audioMode = it }
        }
    }
    private val resetButton = JButton("Reset").apply {
        toolTipText = "Reset reading position to 0 and clear transcription / audio queue"
        addActionListener {
            nettalk.reset()
        }
    }

    init {
        layout = MigLayout("fillx, wrap 1, ins 8, gapy 4")

        warnIfEspeakUnavailable()

        val textScroll = JScrollPane(textArea).apply {
            preferredSize = Dimension(560, 110)
        }
        val textPanel = JPanel(MigLayout("fillx, wrap 1, ins 4, gapy 4")).apply {
            border = BorderFactory.createTitledBorder("Reading text")
            add(textScroll, "growx")
            add(resetButton, "alignx right")
        }

        val toolbar = JPanel(MigLayout("ins 0, gapx 6")).apply {
            add(JLabel("Audio mode:"))
            add(modeBox)
        }

        add(textPanel, "growx")
        add(JLabel("Window (current letter centered):"))
        add(windowLabel, "alignx center, gapy 0 0")
        add(phonemeReadout, "alignx center, gapy 4 4")
        add(JLabel("Articulatory features:"), "gapy 6 0")
        add(featureBars, "growx")
        add(JLabel("Transcription so far:"), "gapy 6 0")
        add(transcriptionScroll, "growx, h 64!")
        val synthHolder = JPanel(MigLayout("fillx, ins 0, wrap 1")).apply {
            add(synthEditor.createApplyPanel(), "growx")
        }
        add(DetailTrianglePanel(
            contentPanel = synthHolder,
            defaultOpen = false,
            upLabel = "Speech synthesizer",
            downLabel = "Speech synthesizer"
        ), "growx, gapy 6 0")
        add(toolbar, "gapy 4 0")

        textArea.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = syncFromArea()
            override fun insertUpdate(e: DocumentEvent) = syncFromArea()
            override fun removeUpdate(e: DocumentEvent) = syncFromArea()
        })

        nettalk.events.textChanged.on(Dispatchers.Swing) {
            if (textArea.text != nettalk.text) textArea.text = nettalk.text
            updateHighlights()
            updateReadout()
        }
        nettalk.events.positionChanged.on(Dispatchers.Swing) {
            updateHighlights()
            updateReadout()
        }
        nettalk.events.decoded.on(Dispatchers.Swing) {
            updateReadout()
        }
        nettalk.events.transcriptionChanged.on(Dispatchers.Swing) {
            updateReadout()
        }
        nettalk.events.audioModeChanged.on(Dispatchers.Swing) {
            if (modeBox.selectedItem != nettalk.audioMode) modeBox.selectedItem = nettalk.audioMode
        }
        nettalk.events.audioSegmentChanged.on(Dispatchers.Swing) {
            updateHighlights()
        }

        updateHighlights()
        updateReadout()
    }

    private fun syncFromArea() {
        if (textArea.text != nettalk.text) {
            nettalk.text = textArea.text
        }
    }

    private fun updateHighlights() {
        try {
            val areaHighlighter = textArea.highlighter
            areaHighlighter.removeAllHighlights()
            val len = nettalk.text.length
            if (len > 0) {
                val pos = displayPosition().coerceIn(0, len - 1)
                areaHighlighter.addHighlight(pos, (pos + 1).coerceAtMost(len), cursorPainter)
                nettalk.currentlyPlayingSegment?.range?.let { range ->
                    val start = range.first.coerceIn(0, len)
                    val end = (range.last + 1).coerceIn(start, len)
                    if (end > start) areaHighlighter.addHighlight(start, end, speakingPainter)
                }
            }
            updateTranscriptionHighlights()
        } catch (_: Exception) {
        }
    }

    /**
     * The text position the user perceives as "currently being processed". Defaults to
     * `lastDecodedPosition` (the letter whose phoneme/IPA the readout is showing right now)
     * so the window center, yellow highlight, and readout all agree. Falls back to the live
     * `position` before any phoneme has been decoded.
     */
    private fun displayPosition(): Int {
        val ldp = nettalk.lastDecodedPosition
        return if (ldp >= 0) ldp else nettalk.position
    }

    private fun updateTranscriptionHighlights() {
        val h = transcriptionArea.highlighter
        h.removeAllHighlights()
        val tLen = transcriptionArea.document.length
        if (tLen == 0) return
        nettalk.lastDecodedTranscriptionRange?.let { r ->
            val start = r.first.coerceIn(0, tLen)
            val end = (r.last + 1).coerceIn(start, tLen)
            if (end > start) h.addHighlight(start, end, cursorPainter)
        }
        var greenStart = -1
        nettalk.currentlyPlayingSegment?.range?.let { textRange ->
            nettalk.transcriptionRangeForTextRange(textRange)?.let { tr ->
                val start = tr.first.coerceIn(0, tLen)
                val end = (tr.last + 1).coerceIn(start, tLen)
                if (end > start) {
                    h.addHighlight(start, end, speakingPainter)
                    greenStart = start
                }
            }
        }
        scrollTranscriptionTo(if (greenStart >= 0) greenStart else tLen)
    }

    private fun scrollTranscriptionTo(offset: Int) {
        try {
            val rect = transcriptionArea.modelToView2D(offset)?.bounds ?: return
            transcriptionArea.scrollRectToVisible(rect)
        } catch (_: Exception) {
        }
    }

    private fun updateReadout() {
        val ws = nettalk.windowSize
        val pad = "_".repeat(ws / 2)
        val padded = pad + nettalk.text.lowercase() + pad
        val pos = displayPosition()
        val window = if (padded.length >= pos + ws) {
            padded.substring(pos, pos + ws)
        } else {
            padded.padEnd(pos + ws, '_').substring(pos, pos + ws)
        }
        val center = ws / 2
        val highlightedWindow = buildString {
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
        windowLabel.text = highlightedWindow

        val letter = nettalk.text.getOrNull(displayPosition())?.toString()?.ifBlank { "·" } ?: "·"
        val phonChar = nettalk.predictedPhoneme.firstOrNull() ?: '-'
        val phon = if (phonChar == '-') "·" else phonChar.toString()
        val ipa = NettalkPhonology.toIpa[phonChar] ?: "·"

        val stress = nettalk.lastStress
        val stressLabel = when (stress) {
            '>' -> "boundary →"
            '<' -> "← boundary"
            '0' -> "unstressed"
            '1' -> "primary"
            '2' -> "secondary"
            else -> stress.toString()
        }

        phonemeReadout.text = buildString {
            append("<html><table cellpadding='2' cellspacing='6' style='font-family:sans-serif'>")
            append("<tr>")
            append(cell("letter", letter, "#aa8800", width = 60))
            append("<td valign='middle' align='center' width='28' style='font-size:14pt;color:#888'>&rarr;</td>")
            append(cell("phoneme", phon, "#1c5fa1", width = 60))
            append(cell("IPA", ipa, "#3a7d3a", width = 70))
            append(cell("stress", stressLabel, "#666666", small = true, width = 120))
            append("</tr></table></html>")
        }

        featureBars.values = nettalk.articulatoryFeatures
        featureBars.repaint()

        val txt = nettalk.transcription.toString()
        if (transcriptionArea.text != txt) {
            transcriptionArea.text = txt
        }
        updateTranscriptionHighlights()
    }

    private fun cell(label: String, value: String, color: String, small: Boolean = false, width: Int = 80): String {
        val valueSize = if (small) "13pt" else "20pt"
        return "<td width='$width' align='center' valign='middle'>" +
            "<div style='font-size:9pt;color:#888;text-transform:uppercase;letter-spacing:1px'>$label</div>" +
            "<div style='font-size:$valueSize;color:$color'><b>$value</b></div>" +
            "</td>"
    }

    private fun escapeHtml(c: Char): String = when (c) {
        '<' -> "&lt;"
        '>' -> "&gt;"
        '&' -> "&amp;"
        ' ' -> "&middot;"
        else -> c.toString()
    }

    /**
     * Custom panel that renders the 21 articulatory features and 5 stress features as
     * adjacent vertical bars, with rotated labels and subtle separators between
     * feature groups (place / manner / voicing / vowel-position / tense / silent / stress).
     */
    private class FeatureBarsPanel : JPanel() {
        var values: DoubleArray = DoubleArray(NettalkPhonology.outputDimension)
        private val labels = NettalkPhonology.featureNames + NettalkPhonology.stressNames

        // Indices at which a new feature group begins. Used to draw separators.
        private val groupBoundaries = setOf(6, 12, 13, 19, 20, 21, NettalkPhonology.numArticulatoryFeatures)

        init {
            preferredSize = Dimension(560, 110)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val n = values.size.coerceAtLeast(1)
            val labelArea = 50
            val barHeight = (height - labelArea).coerceAtLeast(1)
            val w = width.toDouble() / n
            val barGap = (w * 0.30).toInt().coerceIn(2, 10)
            val articulatory = NettalkPhonology.numArticulatoryFeatures
            val labelFont = Font(Font.SANS_SERIF, Font.PLAIN, 11)
            g2.font = labelFont
            for (i in 0 until n) {
                val v = values[i].coerceIn(0.0, 1.0)
                val h = (v * barHeight).toInt()
                val slotStart = (i * w).toInt()
                val slotEnd = ((i + 1) * w).toInt()
                val barX = slotStart + barGap / 2
                val barW = (slotEnd - slotStart - barGap).coerceAtLeast(1)
                g2.color = if (i < articulatory) Color(80, 130, 200) else Color(200, 100, 80)
                g2.fillRect(barX, barHeight - h, barW, h)
                g2.color = Color(235, 235, 235)
                g2.drawRect(barX, 0, barW, barHeight)
                if (i in groupBoundaries) {
                    g2.color = Color(190, 190, 190)
                    g2.drawLine(slotStart, 2, slotStart, barHeight - 2)
                }
                if (i < labels.size) {
                    val label = labels[i]
                    val saved = g2.transform
                    g2.color = Color.DARK_GRAY
                    g2.translate((barX + barW / 2.0) - 2, (barHeight + 4).toDouble())
                    g2.rotate(Math.PI / 4)
                    g2.drawString(label, 0, 0)
                    g2.transform = saved
                }
            }
        }
    }
}
