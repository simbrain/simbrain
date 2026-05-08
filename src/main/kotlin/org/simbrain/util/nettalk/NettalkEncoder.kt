package org.simbrain.util.nettalk

/**
 * NETtalk-style sliding-window encoder for English text.
 *
 * Each input position is encoded as a 29-bit one-hot:
 *   - 26 units for `a`..`z`
 *   - 1 unit for word boundary / blank (used to pad both sides of words)
 *   - 1 unit for `.` end-of-sentence punctuation
 *   - 1 unit for `,` mid-sentence punctuation (any other ASCII punctuation collapses here)
 *
 * The window has [windowSize] consecutive positions (default 7), with the central position
 * being the "current" letter the network is asked to pronounce. Three letters of left and
 * right context (for the default size of 7) help disambiguate pronunciation. Outside the
 * word, positions are filled with the blank symbol.
 *
 * Outputs use [NettalkPhonology] for the phoneme + stress encoding (26 units total).
 */
class NettalkEncoder(val windowSize: Int = 7) {

    init {
        require(windowSize >= 1 && windowSize % 2 == 1) {
            "Window size must be a positive odd integer; got $windowSize"
        }
    }

    val halfWindow: Int = windowSize / 2

    val perPositionInputSize: Int = 29

    val inputSize: Int get() = windowSize * perPositionInputSize

    val outputSize: Int get() = NettalkPhonology.outputDimension

    private fun letterIndex(c: Char): Int = when {
        c in 'a'..'z' -> c - 'a'
        c == ' ' || c == '\t' || c == '\n' -> 26
        c == '.' || c == '!' || c == '?' -> 27
        else -> 28
    }

    /** Encode one window position as a 29-D one-hot. */
    fun encodePosition(c: Char): DoubleArray {
        val v = DoubleArray(perPositionInputSize)
        v[letterIndex(c.lowercaseChar())] = 1.0
        return v
    }

    /**
     * Encode a 7-letter window centered on [position] within [word]. Out-of-bounds positions
     * are filled with the blank symbol. Non-letter characters in the word collapse via
     * [letterIndex].
     */
    fun encodeWindow(word: String, position: Int): DoubleArray {
        val v = DoubleArray(inputSize)
        for (i in 0 until windowSize) {
            val src = position - halfWindow + i
            val c = if (src in word.indices) word[src].lowercaseChar() else ' '
            v[i * perPositionInputSize + letterIndex(c)] = 1.0
        }
        return v
    }

    /**
     * Encode one [NettalkEntry] into a list of (input, target) rows, one per letter position.
     */
    fun encodeEntry(entry: NettalkEntry): List<Pair<DoubleArray, DoubleArray>> {
        return List(entry.word.length) { i ->
            encodeWindow(entry.word, i) to NettalkPhonology.encodeOutput(entry.phonemes[i], entry.stress[i])
        }
    }

    /**
     * Encode a list of entries as a flat training set, treating each word in isolation.
     * Windows that extend past a word's edges are padded with blanks. Faithful to the
     * "dictionary corpus" training described in Sejnowski & Rosenberg (1987), but means
     * the network never sees inter-word context and may struggle on running text.
     */
    fun encodeEntries(entries: List<NettalkEntry>): TrainingTensors {
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<DoubleArray>()
        val rowLabels = mutableListOf<String>()
        for (entry in entries) {
            for (i in entry.word.indices) {
                inputs.add(encodeWindow(entry.word, i))
                targets.add(NettalkPhonology.encodeOutput(entry.phonemes[i], entry.stress[i]))
                rowLabels.add("${entry.word}[$i:'${entry.word[i]}']")
            }
        }
        return TrainingTensors(inputs, targets, rowLabels)
    }

    /**
     * Encode a list of entries as a single continuous text stream, with words separated by
     * [gap] spaces. Windows naturally span word boundaries, so the network is trained on the
     * same kind of context it will see when reading running text. Targets are produced for
     * every position — letter positions get the entry's phoneme/stress, gap (space)
     * positions get `-` / `<` so the network learns "silent" for word boundaries explicitly
     * rather than relying on extrapolation.
     *
     * If [repeats] > 1, the entries are concatenated [repeats] times. When [shuffleSeed] is
     * non-null, each pass is shuffled independently — so the same word appears with
     * different neighbors across passes, exposing the network to varied padding/context
     * configurations.
     */
    fun encodeAsContinuousText(
        entries: List<NettalkEntry>,
        gap: Int = 1,
        repeats: Int = 1,
        shuffleSeed: Long? = null
    ): TrainingTensors {
        require(gap >= 1) { "gap must be >= 1, got $gap" }
        require(repeats >= 1) { "repeats must be >= 1, got $repeats" }
        val text = StringBuilder()
        val phonemes = StringBuilder()
        val stress = StringBuilder()
        var firstAppend = true
        for (pass in 0 until repeats) {
            val ordered = if (shuffleSeed != null) {
                entries.shuffled(kotlin.random.Random(shuffleSeed + pass))
            } else {
                entries
            }
            for (entry in ordered) {
                if (!firstAppend) {
                    repeat(gap) {
                        text.append(' ')
                        phonemes.append('-')
                        stress.append('<')
                    }
                }
                firstAppend = false
                text.append(entry.word)
                phonemes.append(entry.phonemes)
                stress.append(entry.stress)
            }
        }
        val flatText = text.toString()
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<DoubleArray>()
        val rowLabels = mutableListOf<String>()
        for (i in flatText.indices) {
            inputs.add(encodeWindow(flatText, i))
            targets.add(NettalkPhonology.encodeOutput(phonemes[i], stress[i]))
            rowLabels.add("'${flatText[i]}'@$i")
        }
        return TrainingTensors(inputs, targets, rowLabels)
    }
}

/**
 * Flat training tensors produced by [NettalkEncoder.encodeEntries].
 */
data class TrainingTensors(
    val inputs: List<DoubleArray>,
    val targets: List<DoubleArray>,
    val rowLabels: List<String>
)
