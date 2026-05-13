package org.simbrain.custom_sims.simulations.nettalk

import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.addSpeechSynthesizer
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.world.speechsynthesizer.SpeechSynthesizer
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Demo for the eSpeak-ng speech synthesizer.
 *
 * Provides a control panel for sending plain text or phoneme strings to a
 * [SpeechSynthesizer]. Used to verify the speech pipeline works end-to-end before
 * wiring it into a real NETtalk simulation.
 */
val synthesizerDemo = newSim {

    workspace.clearWorkspace()

    val synthesizer = SpeechSynthesizer()
    val speechWorld = addSpeechSynthesizer("Speech Synthesizer", synthesizer)

    var wordToSpeak = "hello how are you"
    var phonemesToSpeak = "h@l'oU h'aU A: j'u:"

    withGui {
        val controlPanel = createControlPanel("Speak", 0, 0) {
            addComponent(textAreaPanel("Text", wordToSpeak) { wordToSpeak = it })
            addButton("Speak text") {
                synthesizer.speakText(wordToSpeak.trim())
            }

            addSeparator()

            addComponent(textAreaPanel("Phonemes", phonemesToSpeak) { phonemesToSpeak = it })
            addButton("Speak phonemes") {
                synthesizer.speakPhonemes(phonemesToSpeak.trim())
            }
        }
        controlPanel.setBounds(0, 3, 324, 388)
        place(speechWorld, 310, 2, 605, 593)
    }

    addSidebarInfo(
        """
        # Synthesizer Demo

        This demo shows the two ways Simbrain can synthesize speech:
        ordinary text-to-speech in the top panel, and direct phoneme synthesis in the
        bottom panel. 
        
        The synthesizer will probably eventually be turned into a standard component available from the GUI.

        # Basic Text-To-Speech

        Text-to-speech converts written words into audible speech. A synthesizer first
        decides how the text should be pronounced, then generates the audio waveform for
        that pronunciation. Simbrain uses [eSpeak NG](https://github.com/espeak-ng/espeak-ng),
        a compact open-source speech synthesizer, for this basic text-to-speech path.

        Enter a word or phrase in `Text` and click `Speak text`.

        # Phoneme Synthesis

        Phoneme synthesis bypasses the ordinary text-to-pronunciation step. Instead of
        asking the synthesizer to infer how a word should sound, Simbrain sends an explicit
        sequence of speech sounds. This is useful when you want precise control over the
        sounds being generated or when another model has already produced a phonetic
        representation.

        The `Phonemes` field uses eSpeak-ng's
        [Kirshenbaum notation](https://chromium.googlesource.com/chromiumos/third_party/espeak-ng/+/HEAD/docs/phonemes/kirshenbaum.md),
        an ASCII notation for phonetic sounds. 

        Enter an eSpeak-ng phoneme string in `Phonemes` and click `Speak phonemes`.

        # Examples

        - `h@l'oU` for "hello"
        - `w'3rld` for "world"
        - `n'jU@r@l` for "neural"
        - `n'Etw3rk` for "network"
        - `sEjn'aUski` for "Sejnowski"
        - `f'oUnim` for "phoneme"
        - `b'@b@l` for "babble"
        - `k@mpj'u:t@` for "computer"
        """.trimIndent()
    )
}

private fun textAreaPanel(label: String, initialText: String, onChange: (String) -> Unit): JPanel {
    val textArea = JTextArea(initialText, 4, 28).apply {
        lineWrap = true
        wrapStyleWord = true
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onChange(text)
            override fun removeUpdate(e: DocumentEvent) = onChange(text)
            override fun changedUpdate(e: DocumentEvent) = onChange(text)
        })
    }
    return JPanel(BorderLayout()).apply {
        add(JLabel(label), BorderLayout.NORTH)
        add(JScrollPane(textArea).apply {
            preferredSize = Dimension(280, 90)
        }, BorderLayout.CENTER)
    }
}
