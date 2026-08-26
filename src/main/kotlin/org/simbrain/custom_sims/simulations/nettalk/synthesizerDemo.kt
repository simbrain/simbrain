package org.simbrain.custom_sims.simulations.nettalk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.place
import org.simbrain.world.speechsynthesizer.SpeechSynthesizer
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
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
        val controlPanel = createControlPanel("Speak", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComponent(textAreaPanel("Text", wordToSpeak) { wordToSpeak = it })
            addButton("Speak text") {
                synthesizer.speakText(wordToSpeak.trim())
            }

            addSeparator()

            addComponent(textAreaPanel("Phonemes", phonemesToSpeak) { phonemesToSpeak = it })
            addButton("Speak phonemes") {
                synthesizer.speakPhonemes(phonemesToSpeak.trim())
            }

            addSeparator()

            addComponent(featureVectorPanel(synthesizer))
        }.awaitLayout()
        controlPanel.setBounds(SIM_WINDOW_GAP, SIM_WINDOW_GAP, 324, 570)
        place(speechWorld, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 605, 593)
    }

    addSidebarInfo(
        """
        # Synthesizer Demo

        This demo shows three ways Simbrain can synthesize speech: ordinary text,
        explicit phoneme strings, and articulatory feature vectors. The `Speak` panel on
        the left sends examples to the `Speech Synthesizer` component on the right.

        In the synthesizer component, `Currently speaking` shows the utterance being
        played, `History` shows what has been sent to the synthesizer, and `Most recent
        feature vector` shows the last articulatory vector received. Open `Settings` to
        experiment with voices, accents, speed, pitch, amplitude, and feature-vector
        buffering.
        
        Note: problems with this sim have been reported on some machines, which should be fixed in a release soon.

        # Simulation Details

        ## Basic Text-To-Speech

        Text-to-speech converts written words into audible speech. A synthesizer first
        decides how the text should be pronounced, then generates the audio waveform for
        that pronunciation. Simbrain uses [eSpeak NG](https://github.com/espeak-ng/espeak-ng),
        a compact open-source speech synthesizer, for this basic text-to-speech path.

        Enter a word or phrase in `Text` and click `Speak text`.

        ## Phoneme Synthesis

        Phoneme synthesis bypasses the ordinary text-to-pronunciation step. Instead of
        asking the synthesizer to infer how a word should sound, Simbrain sends an explicit
        sequence of speech sounds. This is useful when you want precise control over the
        sounds being generated or when another model has already produced a phonetic
        representation.

        The `Phonemes` field uses eSpeak-ng's
        [Kirshenbaum notation](https://chromium.googlesource.com/chromiumos/third_party/espeak-ng/+/HEAD/docs/phonemes/kirshenbaum.md),
        an ASCII notation for phonetic sounds. 

        Enter an eSpeak-ng phoneme string in `Phonemes` and click `Speak phonemes`.

        ## Articulatory Feature Synthesis

        Feature-vector synthesis bypasses both ordinary text and phoneme strings. The
        demo constructs a 26-dimensional feature vector from a selected phoneme and
        stress marker, then sends it directly to the synthesizer. This representation is
        inspired by the NETtalk reading-aloud model used in the separate `NETtalk`
        simulation, but here it is just a compact way to demonstrate feature-vector
        speech input.

        The feature vector is not a raw acoustic speech signal. It is a symbolic
        articulatory description: 21 binary features describe properties such as place,
        manner, voicing, and vowel quality, while 5 stress features encode syllable/stress
        information.

        When a feature vector is spoken, Simbrain finds the nearest known phoneme pattern
        and sends that phoneme to [eSpeak NG](https://github.com/espeak-ng/espeak-ng).
        Random or intermediate network outputs can still be decoded, but they are
        interpreted by nearest-phoneme match rather than by generating sound directly from
        the vector.

        # What to Do

        ## Examples

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

private fun featureVectorPanel(synthesizer: SpeechSynthesizer): JPanel {
    val speakScope = CoroutineScope(Dispatchers.Default)
    var phoneme = 'a'
    var stress = '0'

    val phonemeSelector = JComboBox(NettalkPhonology.phonemeFeatures.keys.toTypedArray()).apply {
        selectedItem = phoneme
    }
    val stressSelector = JComboBox(NettalkPhonology.stressNames.map { it[0] }.toTypedArray()).apply {
        selectedItem = stress
    }
    val vectorArea = JTextArea(3, 28).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    fun currentVector() = NettalkPhonology.encodeOutput(phoneme, stress)

    fun updatePreview() {
        vectorArea.text = currentVector().joinToString(prefix = "[", postfix = "]") { it.toInt().toString() }
        vectorArea.caretPosition = 0
    }

    phonemeSelector.addActionListener {
        phoneme = phonemeSelector.selectedItem as Char
        updatePreview()
    }
    stressSelector.addActionListener {
        stress = stressSelector.selectedItem as Char
        updatePreview()
    }

    updatePreview()

    return JPanel(BorderLayout()).apply {
        add(JLabel("Articulatory feature vector"), BorderLayout.NORTH)
        add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(JLabel("Phoneme"))
            add(phonemeSelector)
            add(JLabel("Stress"))
            add(stressSelector)
        }, BorderLayout.CENTER)
        add(JPanel(BorderLayout()).apply {
            add(JScrollPane(vectorArea).apply {
                preferredSize = Dimension(280, 70)
            }, BorderLayout.CENTER)
            add(JButton("Speak feature vector").apply {
                addActionListener {
                    // Raw Swing listener; the suspend speech path paces itself off the event thread
                    speakScope.launch {
                        synthesizer.speakFeatureVector(currentVector())
                        synthesizer.flushFeatureBuffer()
                    }
                }
            }, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
    }
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
