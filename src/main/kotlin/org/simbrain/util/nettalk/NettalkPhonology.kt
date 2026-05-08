package org.simbrain.util.nettalk

/**
 * Articulatory feature representation for the NETtalk phoneme set, as used in
 * Sejnowski & Rosenberg (1987) "Parallel networks that learn to pronounce English text".
 *
 * Each phoneme is encoded as a 21-bit articulatory feature vector. Stress / syllable
 * information is encoded separately as a 5-bit one-hot vector. Combined output dimension
 * is 26.
 *
 * The articulatory feature scheme used here is a modern reconstruction of the original
 * scheme; specific bit assignments may differ in detail from the 1987 paper but capture
 * the same linguistic structure (place, manner, voicing, vowel height/backness, tenseness,
 * silence/continuation/punctuation).
 */
object NettalkPhonology {

    val featureNames = listOf(
        "labial", "dental", "alveolar", "palatal", "velar", "glottal",
        "stop", "nasal", "fricative", "affricate", "glide", "liquid",
        "voiced",
        "high", "medium", "low", "front", "central", "back",
        "tense",
        "silent"
    )

    val numArticulatoryFeatures: Int get() = featureNames.size

    val stressNames = listOf(">", "<", "0", "1", "2")

    val numStressFeatures: Int get() = stressNames.size

    val outputDimension: Int get() = numArticulatoryFeatures + numStressFeatures

    private fun feat(vararg fs: String): IntArray {
        val v = IntArray(numArticulatoryFeatures)
        for (f in fs) {
            val idx = featureNames.indexOf(f)
            require(idx >= 0) { "Unknown feature: $f" }
            v[idx] = 1
        }
        return v
    }

    /**
     * 21-bit articulatory feature vector for each NETtalk phoneme.
     * Includes all 50 phonemes from the corpus plus `-` for silent/continuation.
     */
    val phonemeFeatures: Map<Char, IntArray> = mapOf(
        '-' to feat("silent"),

        'a' to feat("voiced", "low", "back"),
        '@' to feat("voiced", "low", "front"),
        'E' to feat("voiced", "medium", "front"),
        'I' to feat("voiced", "high", "front"),
        'i' to feat("voiced", "high", "front", "tense"),
        '^' to feat("voiced", "medium", "central"),
        'c' to feat("voiced", "medium", "back", "tense"),
        'u' to feat("voiced", "high", "back", "tense"),
        'U' to feat("voiced", "high", "back"),
        'x' to feat("voiced", "central"),
        'R' to feat("voiced", "medium", "central", "liquid"),

        'e' to feat("voiced", "medium", "front", "tense", "glide"),
        'A' to feat("voiced", "low", "front", "tense", "glide"),
        'o' to feat("voiced", "medium", "back", "tense", "glide"),
        'O' to feat("voiced", "medium", "back", "glide"),
        'W' to feat("voiced", "low", "central", "tense", "glide"),
        'Y' to feat("voiced", "high", "front", "tense", "glide"),
        '+' to feat("voiced", "low", "central", "glide"),

        'p' to feat("labial", "stop"),
        'b' to feat("labial", "stop", "voiced"),
        't' to feat("alveolar", "stop"),
        'd' to feat("alveolar", "stop", "voiced"),
        'k' to feat("velar", "stop"),
        'g' to feat("velar", "stop", "voiced"),

        'm' to feat("labial", "nasal", "voiced"),
        'n' to feat("alveolar", "nasal", "voiced"),
        'G' to feat("velar", "nasal", "voiced"),

        'L' to feat("alveolar", "liquid", "voiced", "medium"),
        'M' to feat("labial", "nasal", "voiced", "medium"),
        'N' to feat("alveolar", "nasal", "voiced", "medium"),

        'f' to feat("labial", "fricative"),
        'v' to feat("labial", "fricative", "voiced"),
        'T' to feat("dental", "fricative"),
        'D' to feat("dental", "fricative", "voiced"),
        's' to feat("alveolar", "fricative"),
        'z' to feat("alveolar", "fricative", "voiced"),
        'S' to feat("palatal", "fricative"),
        'Z' to feat("palatal", "fricative", "voiced"),
        'h' to feat("glottal", "fricative"),

        'C' to feat("palatal", "affricate"),
        'J' to feat("palatal", "affricate", "voiced"),
        'K' to feat("velar", "palatal", "affricate"),
        'X' to feat("velar", "alveolar", "affricate"),
        '!' to feat("alveolar", "affricate"),
        '#' to feat("velar", "alveolar", "affricate", "voiced"),

        'w' to feat("labial", "glide", "voiced"),
        'y' to feat("palatal", "glide", "voiced"),
        '*' to feat("labial", "glide"),

        'l' to feat("alveolar", "liquid", "voiced"),
        'r' to feat("alveolar", "liquid", "voiced", "central")
    )

    /**
     * One-hot 5-bit stress vector keyed by the stress character.
     */
    val stressFeatures: Map<Char, IntArray> = stressNames.mapIndexed { i, name ->
        name[0] to IntArray(numStressFeatures).also { it[i] = 1 }
    }.toMap()

    /**
     * Concatenated 26-D output vector for a (phoneme, stress) pair.
     */
    fun encodeOutput(phoneme: Char, stress: Char): DoubleArray {
        val phon = phonemeFeatures[phoneme] ?: error("Unknown phoneme: '$phoneme'")
        val str = stressFeatures[stress] ?: error("Unknown stress char: '$stress'")
        val out = DoubleArray(outputDimension)
        for (i in phon.indices) out[i] = phon[i].toDouble()
        for (i in str.indices) out[numArticulatoryFeatures + i] = str[i].toDouble()
        return out
    }

    /**
     * Decode a 26-D output vector to its closest (phoneme, stress) pair by Euclidean distance
     * over the articulatory portion and one-hot argmax over the stress portion.
     */
    fun decodeOutput(output: DoubleArray): Pair<Char, Char> {
        require(output.size == outputDimension) {
            "Expected $outputDimension outputs, got ${output.size}"
        }
        val phonPart = output.sliceArray(0 until numArticulatoryFeatures)
        var bestPhon = '-'
        var bestPhonDist = Double.POSITIVE_INFINITY
        for ((p, vec) in phonemeFeatures) {
            var d = 0.0
            for (i in vec.indices) {
                val diff = phonPart[i] - vec[i]
                d += diff * diff
            }
            if (d < bestPhonDist) {
                bestPhonDist = d
                bestPhon = p
            }
        }
        var bestStressIdx = 0
        var bestStressVal = output[numArticulatoryFeatures]
        for (i in 1 until numStressFeatures) {
            val v = output[numArticulatoryFeatures + i]
            if (v > bestStressVal) {
                bestStressVal = v
                bestStressIdx = i
            }
        }
        return bestPhon to stressNames[bestStressIdx][0]
    }

    /**
     * NETtalk phoneme symbols mapped to eSpeak-ng's Kirshenbaum phoneme notation
     * (the form accepted inside `[[ ]]` brackets). `-` (silent) and stress markers
     * have no spoken realization and are absent from the map.
     *
     * Some NETtalk symbols are clusters that don't map to a single eSpeak phoneme;
     * those are mapped to the nearest sequence of eSpeak phonemes.
     */
    val toEspeak: Map<Char, String> = mapOf(
        'a' to "A:",
        '@' to "a",
        'e' to "eI",
        'i' to "i:",
        'I' to "I",
        'o' to "oU",
        'O' to "OI",
        'u' to "u:",
        'U' to "U",
        'E' to "E",
        'A' to "aI",
        'W' to "aU",
        'Y' to "ju:",
        '^' to "V",
        'x' to "@",
        'c' to "O:",
        'R' to "3:",
        '+' to "wa",

        'L' to "@l",
        'M' to "@m",
        'N' to "@n",

        'p' to "p", 'b' to "b",
        't' to "t", 'd' to "d",
        'k' to "k", 'g' to "g",

        'm' to "m", 'n' to "n",
        'G' to "N",

        'f' to "f", 'v' to "v",
        'T' to "T", 'D' to "D",
        's' to "s", 'z' to "z",
        'S' to "S", 'Z' to "Z",
        'h' to "h",

        'C' to "tS", 'J' to "dZ",
        'K' to "kS", 'X' to "ks",
        '!' to "ts", '#' to "gz",

        'w' to "w", 'y' to "j",
        '*' to "w",

        'l' to "l", 'r' to "r"
    )

    /**
     * Convert a NETtalk phoneme string (e.g. `h@l-o-`) into an eSpeak Kirshenbaum
     * phoneme string suitable for speaking inside `[[ ]]`. Silent positions (`-`)
     * are dropped.
     */
    fun nettalkToEspeak(phonemes: String): String =
        phonemes.asSequence()
            .filter { it != '-' }
            .mapNotNull { toEspeak[it] }
            .joinToString("")

    /**
     * NETtalk phoneme symbol → IPA transcription. Diphthongs and clusters use the
     * standard multi-character IPA forms (e.g. `eɪ`, `tʃ`, `l̩`). Useful for displaying
     * a more readable phonetic equivalent in UIs. Silent and stress markers are absent.
     */
    val toIpa: Map<Char, String> = mapOf(
        'a' to "ɑ",
        '@' to "æ",
        'e' to "eɪ",
        'i' to "i",
        'I' to "ɪ",
        'o' to "oʊ",
        'O' to "ɔɪ",
        'u' to "u",
        'U' to "ʊ",
        'E' to "ɛ",
        'A' to "aɪ",
        'W' to "aʊ",
        'Y' to "ju",
        '^' to "ʌ",
        'x' to "ə",
        'c' to "ɔ",
        'R' to "ɝ",
        '+' to "wɑ",

        'L' to "l̩",
        'M' to "m̩",
        'N' to "n̩",

        'p' to "p", 'b' to "b",
        't' to "t", 'd' to "d",
        'k' to "k", 'g' to "ɡ",

        'm' to "m", 'n' to "n",
        'G' to "ŋ",

        'f' to "f", 'v' to "v",
        'T' to "θ", 'D' to "ð",
        's' to "s", 'z' to "z",
        'S' to "ʃ", 'Z' to "ʒ",
        'h' to "h",

        'C' to "tʃ", 'J' to "dʒ",
        'K' to "kʃ", 'X' to "ks",
        '!' to "ts", '#' to "ɡz",

        'w' to "w", 'y' to "j",
        '*' to "ʍ",

        'l' to "l", 'r' to "ɹ"
    )

    /** Convert a NETtalk phoneme string into IPA. Silent positions (`-`) are dropped. */
    fun nettalkToIpa(phonemes: String): String =
        phonemes.asSequence()
            .filter { it != '-' }
            .mapNotNull { toIpa[it] }
            .joinToString("")
}
