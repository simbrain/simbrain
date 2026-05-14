package org.simbrain.world.speechsynthesizer.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.speechsynthesizer.SpeechSynthesizer
import org.simbrain.world.speechsynthesizer.warnIfEspeakUnavailable
import java.awt.*
import javax.swing.*

class SpeechSynthesizerPanel(private val synthesizer: SpeechSynthesizer) : JPanel() {

    private val editor = AnnotatedPropertyEditor(listOf(synthesizer)).also { editor ->
        editor.parameterWidgetMap.values.forEach { widget ->
            widget.events.valueChanged.on {
                editor.commitChanges()
                updateFeatureVisibility()
            }
        }
    }

    private val currentLabel = JLabel(" ").apply {
        font = Font(Font.SANS_SERIF, Font.BOLD, 15)
    }

    private val transcriptionArea = JTextArea(4, 54).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val featureBars = FeatureBarsPanel(synthesizer)

    private val featureLabel = JLabel("Most recent feature vector:")

    init {
        layout = MigLayout("fillx, wrap 1, ins 8, gapy 6, hidemode 3")
        warnIfEspeakUnavailable()

        add(editor, "growx")
        add(JLabel("Currently speaking:"))
        add(currentLabel, "growx")
        add(JLabel("History:"))
        add(JScrollPane(transcriptionArea), "growx, h 96!")
        add(JButton("Clear history").apply {
            addActionListener { synthesizer.clearTranscription() }
        }, "alignx right")
        add(featureLabel)
        add(featureBars, "growx")

        synthesizer.events.speakingChanged.on(Dispatchers.Swing) {
            updateReadout()
        }
        synthesizer.events.transcriptionChanged.on(Dispatchers.Swing) {
            updateReadout()
        }
        synthesizer.events.codecChanged.on(Dispatchers.Swing) {
            updateFeatureVisibility()
            featureBars.repaint()
        }
        synthesizer.events.inputModeChanged.on(Dispatchers.Swing) {
            updateFeatureVisibility()
        }

        updateFeatureVisibility()
        updateReadout()
    }

    private fun updateFeatureVisibility() {
        val showFeatures = synthesizer.inputMode == SpeechSynthesizer.InputMode.ARTICULATORY_FEATURES
        setEditorParameterVisible("Feature decoder", showFeatures)
        setEditorParameterVisible("Buffering", showFeatures)
        setEditorParameterVisible("Max buffer size", showFeatures)
        featureLabel.isVisible = showFeatures
        featureBars.isVisible = showFeatures
        revalidate()
        repaint()
    }

    private fun setEditorParameterVisible(label: String, visible: Boolean) {
        editor.parameterWidgetMap.entries
            .firstOrNull { (parameter, _) -> parameter.label == label }
            ?.let { (parameter, widget) ->
                widget.component.isVisible = visible
                editor.parameterJLabels[parameter]?.isVisible = visible
            }
    }

    private fun updateReadout() {
        currentLabel.text = synthesizer.currentUtterance.ifBlank { " " }
        if (transcriptionArea.text != synthesizer.transcription) {
            transcriptionArea.text = synthesizer.transcription
            transcriptionArea.caretPosition = transcriptionArea.document.length
        }
        featureBars.repaint()
    }

    private class FeatureBarsPanel(private val synthesizer: SpeechSynthesizer) : JPanel() {

        init {
            preferredSize = Dimension(560, 120)
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val values = synthesizer.featureVector
            val codec = synthesizer.codec
            val labels = codec.featureNames + codec.stressNames
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val n = values.size.coerceAtLeast(1)
            val labelArea = 54
            val barHeight = (height - labelArea).coerceAtLeast(1)
            val w = width.toDouble() / n
            val barGap = (w * 0.30).toInt().coerceIn(2, 10)
            g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
            for (i in 0 until n) {
                val value = values[i].coerceIn(0.0, 1.0)
                val h = (value * barHeight).toInt()
                val slotStart = (i * w).toInt()
                val slotEnd = ((i + 1) * w).toInt()
                val barX = slotStart + barGap / 2
                val barW = (slotEnd - slotStart - barGap).coerceAtLeast(1)
                g2.color = if (i < codec.featureNames.size) Color(80, 130, 200) else Color(200, 100, 80)
                g2.fillRect(barX, barHeight - h, barW, h)
                g2.color = Color(235, 235, 235)
                g2.drawRect(barX, 0, barW, barHeight)
                labels.getOrNull(i)?.let { label ->
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
