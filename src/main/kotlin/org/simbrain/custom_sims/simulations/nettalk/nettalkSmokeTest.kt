package org.simbrain.custom_sims.simulations.nettalk

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.soundworld.PhonemeSynthesizer

/**
 * Smoke test for the eSpeak-ng phoneme synthesizer.
 *
 * Provides a control panel with buttons that send phoneme strings to a [PhonemeSynthesizer]
 * and update a [NetworkTextObject] showing what was last spoken. Used to verify the
 * eSpeak-ng subprocess pipeline works end-to-end before wiring it into a real NETtalk
 * simulation.
 */
val nettalkSmokeTest = newSim {

    workspace.clearWorkspace()

    val synthesizer = PhonemeSynthesizer()
    val soundWorld = addSoundWorld("Phoneme Synthesizer", synthesizer)

    val networkComponent = addNetworkComponent("Status")
    val net = networkComponent.network

    val currentStatus = NetworkTextObject("Press a button to speak").apply {
        fontSize = 16
        location = point(0, 0)
    }
    net.addNetworkModel(currentStatus)

    val samples = listOf(
        "hello" to "h@l'oU",
        "world" to "w'3rld",
        "neural" to "n'jU@r@l",
        "network" to "n'Etw3rk",
        "Sejnowski" to "sEjn'aUski",
        "phoneme" to "f'oUnim",
        "babble" to "b'@b@l",
        "computer" to "k@mpj'u:t@"
    )

    withGui {
        place(soundWorld, 0, 0, 400, 300)
        place(networkComponent, 410, 0, 500, 300)

        createControlPanel("Speak", 920, 0) {
            samples.forEach { (label, phonemes) ->
                addButton("$label  [$phonemes]") {
                    currentStatus.text = "Speaking: $label\n[$phonemes]"
                    synthesizer.speakPhonemes(phonemes)
                }
            }
            addButton("Speak word (text mode)") {
                currentStatus.text = "Speaking word: hello (text mode)"
                synthesizer.speakWord("hello")
            }
        }
    }

    addSidebarInfo(
        """
        # Phoneme Synthesizer Smoke Test

        This is a development smoke test for the new `PhonemeSynthesizer`. It verifies that
        Simbrain can drive `espeak-ng` as a subprocess, capture WAV output, and play it through
        the standard Java audio pipeline.

        # What to Do

        1. Make sure `espeak-ng` is installed (`brew install espeak-ng` on macOS).
        2. Click any button in the `Speak` panel to hear the phoneme string.
        3. The status text in the `Status` network shows what was spoken.

        Phoneme strings use eSpeak-ng's Kirshenbaum notation, wrapped in `[[...]]` internally.
        For example, `h@l'oU` is "hello" with primary stress on the second syllable.

        If you hear nothing, check the console for an `espeak-ng not found on PATH` warning.

        # Next Steps

        Once this works end-to-end, the same `PhonemeSynthesizer` will be coupled to the output
        layer of the NETtalk network so the network's predicted phonemes are spoken aloud.
        """.trimIndent()
    )
}
