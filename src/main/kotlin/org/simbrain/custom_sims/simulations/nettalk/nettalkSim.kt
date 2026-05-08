package org.simbrain.custom_sims.simulations.nettalk

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.CharacterTokenizer
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.nettalk.loadNettalkCorpus
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.soundworld.PhonemeSynthesizer
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder

/**
 * NETtalk: Sejnowski & Rosenberg's classic three-layer feed-forward network that learns
 * to pronounce English text. A 7-letter sliding window over a word feeds a hidden layer,
 * which outputs an articulatory feature vector for the central letter. Trained on a
 * 1000-word subset of the bundled NETtalk corpus.
 */
val nettalkSim = newSim {

    workspace.clearWorkspace()

    val numWords = 1000
    val hiddenSize = 80
    val windowSize = 7

    val encoder = NettalkEncoder(windowSize)

    val corpus = loadNettalkCorpus(byFrequency = true)
        .filter { it.isRegular }
        .take(numWords)

    val tensors = encoder.encodeAsContinuousText(corpus, gap = 1, repeats = 3, shuffleSeed = 42L)

    val networkComponent = addNetworkComponent("NETtalk").apply { updateOn = false }
    val net = networkComponent.network

    val bp = BackpropNetwork(intArrayOf(encoder.inputSize, hiddenSize, encoder.outputSize)).apply {
        trainerConfig.learningRate = 0.01
        trainerConfig.testConfiguration.enabled = false
    }
    net.addNetworkModels(bp)

    bp.trainingSet = TrainingDataset(
        inputs = tensors.inputs.map { it.toMutableList() }.toMutableList(),
        targets = tensors.targets.map { it.toMutableList() }.toMutableList(),
        inputRowNames = tensors.rowLabels,
        targetRowNames = tensors.rowLabels,
        targetColumnNames = NettalkPhonology.featureNames + NettalkPhonology.stressNames
    )
    // Replace BackpropNetwork's default randomly-generated testing set with an empty one of
    // the right shape — the NETtalk demo doesn't use validation data.
    bp.testingSet = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = encoder.inputSize,
        targetSize = encoder.outputSize
    )
    bp.initBiases()
    bp.initWeights()

    bp.inputLayer.gridMode = true
    bp.outputLayer.gridMode = true

    val statusText = NetworkTextObject("Click 'Speak word' below.").apply {
        fontSize = 14
        location = point(-350, -260)
    }
    net.addNetworkModel(statusText)

    val phoneticText = NetworkTextObject("").apply {
        fontSize = 18
        isBold = true
        location = point(-350, 280)
    }
    net.addNetworkModel(phoneticText)

    val synth = PhonemeSynthesizer()
    val soundWorld = addSoundWorld("Speech Output", synth)

    val readingText = "the quick brown fox jumps over the lazy dog. " +
        "she sells sea shells by the sea shore. peter piper picked a peck of pickled peppers."
    val textWorldComponent = addTextWorld("Reading Text").apply {
        updateOn = false
        world.tokenizer = CharacterTokenizer(includeWhitespace = true, includePunctuation = true)
        world.autoAdvance = true
        world.text = readingText
        world.tokenEmbedding = TokenEmbeddingBuilder().apply {
            embeddingType = EmbeddingType.OneHot()
        }.build(readingText)
    }
    val textWorld = textWorldComponent.world

    val corpusByWord = corpus.associateBy { it.word }

    var inputWord = "hello"

    suspend fun speakWord(word: String) {
        val cleaned = word.trim().lowercase().filter { it in 'a'..'z' }
        if (cleaned.isEmpty()) {
            statusText.text = "Type a word with at least one letter."
            return
        }
        val predicted = StringBuilder()
        val actual = corpusByWord[cleaned]
        for (i in cleaned.indices) {
            val input = encoder.encodeWindow(cleaned, i)
            bp.inputLayer.setActivations(input)
            networkComponent.update()
            val out = bp.outputLayer.activationArray
            val (p, _) = NettalkPhonology.decodeOutput(out)
            predicted.append(p)
            val pad = "_".repeat(windowSize / 2)
            val padded = pad + cleaned + pad
            val window = padded.substring(i, i + windowSize)
            val highlight = " ".repeat(windowSize / 2) + "^"
            val actualPhon = actual?.phonemes?.getOrNull(i)?.toString() ?: "?"
            statusText.text = "Window: $window\n$highlight\nLetter: '${cleaned[i]}' → predicted '$p', target '$actualPhon'"
        }
        val targetStr = actual?.phonemes ?: "(word not in training set)"
        phoneticText.text = "Word: $cleaned\nPredicted phonemes: $predicted\nTarget phonemes:    $targetStr"
        synth.speakPhonemes(NettalkPhonology.nettalkToEspeak(predicted.toString()))
    }

    workspace.addUpdateAction("Advance reading position") {
        textWorld.update()
    }
    workspace.addUpdateAction("Encode window and run network") {
        val tokens = textWorld.tokens
        if (tokens.isNotEmpty()) {
            val pos = tokens[textWorld.currentTokenIndex].start
            val input = encoder.encodeWindow(textWorld.text, pos)
            bp.inputLayer.setActivations(input)
            networkComponent.update()
        }
    }
    val wordPhonemes = StringBuilder()
    val wordLetters = StringBuilder()

    workspace.addUpdateAction("Decode and speak") {
        val tokens = textWorld.tokens
        if (tokens.isEmpty()) return@addUpdateAction
        val pos = tokens[textWorld.currentTokenIndex].start
        val centerChar = textWorld.text.getOrNull(pos)?.lowercaseChar() ?: ' '
        val out = bp.outputLayer.activationArray
        val (p, _) = NettalkPhonology.decodeOutput(out)
        val pad = "_".repeat(windowSize / 2)
        val padded = pad + textWorld.text.lowercase() + pad
        val window = padded.substring(pos, (pos + windowSize).coerceAtMost(padded.length))
        statusText.text = "Window: $window\n${" ".repeat(windowSize / 2)}^\nLetter: '$centerChar' → predicted '$p'"

        if (centerChar in 'a'..'z') {
            wordLetters.append(centerChar)
            wordPhonemes.append(p)
        } else if (wordPhonemes.isNotEmpty()) {
            phoneticText.text = "Last word: $wordLetters\nPhonemes: $wordPhonemes"
            synth.speakPhonemes(NettalkPhonology.nettalkToEspeak(wordPhonemes.toString()))
            wordPhonemes.clear()
            wordLetters.clear()
        }
    }

    withGui {
        place(networkComponent, 0, 0, 800, 600)
        place(textWorldComponent, 0, 605, 800, 200)
        place(soundWorld, 810, 0, 380, 200)

        createControlPanel("NETtalk Controls", 810, 210) {
            addTextField("Word to speak", inputWord) {
                inputWord = it
            }
            addButton("Speak word") {
                speakWord(inputWord)
            }
            addButton("Speak random training word") {
                inputWord = corpus.random().word
                speakWord(inputWord)
            }
            addSeparator()
            addButton("Reset reading position") {
                textWorld.currentTokenIndex = 0
                wordPhonemes.clear()
                wordLetters.clear()
            }
        }
    }

    addSidebarInfo(
        """
        # NETtalk

        A reimplementation of Sejnowski & Rosenberg's (1987) classic _NETtalk_ — a three-layer
        feed-forward network that learns to pronounce written English. A sliding window of $windowSize
        letters is fed to the network; the network produces an articulatory feature vector
        for the central letter, which is decoded back to a phoneme.

        # Simulation Details

        - Input: $windowSize × 29 = ${encoder.inputSize} units, one-hot per window position (26 letters + 3 punctuation/blank).
        - Hidden: $hiddenSize logistic-sigmoid units.
        - Output: ${encoder.outputSize} logistic-sigmoid units — 21 articulatory features (place, manner,
          voicing, vowel height/backness, tenseness, silence) + 5 stress / syllable bits.
        - Training set: the $numWords most-common regular English words from the bundled NETtalk
          corpus, ordered by frequency rank from the
          [Google 20K English word list](https://github.com/first20hours/google-10000-english).
          Words are concatenated into a single continuous text with single-space separators
          and the sequence is repeated three times with independent shuffles
          (~${tensors.inputs.size} training rows). Windows span word boundaries, and each
          word appears next to different neighbors on each pass — so the network sees a
          variety of context configurations rather than memorizing one fixed neighbor pair.
        - The corpus aligns each English letter with one phoneme; `-` marks silent letters.

        # What to Do

        1. **Speak a word now (untrained).** Type a word in the `NETtalk Controls` panel and click
           `Speak word`. The output will be gibberish — random weights mean random phonemes.
           This is the babbling stage of the original NETtalk demo.

        2. **Train.** Right-click the `NETtalk` network → `Edit/Train Backprop...` and click the
           play button to iterate. The mean error should drop steadily.

        3. **Speak again.** As training progresses, predictions should approximate the target
           pronunciations.

        4. **Read aloud.** Press the workspace `play` button (top toolbar). The network advances
           through the `Reading Text` window one character at a time, decoding a phoneme per
           letter. Phonemes are accumulated within each word and spoken as a unit at word
           boundaries — this gives eSpeak a chance to handle natural transitions between
           neighboring phonemes (coarticulation), so words sound much smoother than per-letter
           audio would. Iteration paces naturally with audio. Use `Reset reading position` in
           the control panel to start over.

        # Audio

        Audio playback uses [eSpeak-ng](https://github.com/espeak-ng/espeak-ng) via a `SoundWorld`
        component. If you don't hear anything, install eSpeak-ng (`brew install espeak-ng` on macOS)
        and restart Simbrain.

        # References

        Sejnowski, T.J., & Rosenberg, C.R. (1987). [Parallel networks that learn to pronounce English text](https://papers.cnl.salk.edu/PDFs/Parallel%20Networks%20that%20Learn%20to%20Pronounce%20English%20Text%201987-3562.pdf). _Complex Systems, 1_, 145–168.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        """.trimIndent()
    )
}
