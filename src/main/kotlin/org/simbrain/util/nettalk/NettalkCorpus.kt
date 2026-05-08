package org.simbrain.util.nettalk

/**
 * One row of the NETtalk corpus: an English [word] aligned position-by-position
 * with its [phonemes] (using NETtalk's 50-symbol DECtalk-derived alphabet, with
 * `-` marking silent / continuation positions) and per-position [stress] markers.
 *
 * [flag] is `0` for ordinary words, `1` for irregular words, `2` for foreign words.
 *
 * Invariant: `word.length == phonemes.length == stress.length`.
 */
data class NettalkEntry(
    val word: String,
    val phonemes: String,
    val stress: String,
    val flag: Int
) {
    init {
        require(word.length == phonemes.length && word.length == stress.length) {
            "Length mismatch in entry '$word': word=${word.length} phonemes=${phonemes.length} stress=${stress.length}"
        }
    }

    val isRegular: Boolean get() = flag == 0
}

/**
 * Parse a NETtalk corpus stream. The corpus is tab-separated `word \t phonemes \t stress \t flag`.
 * Header / comment lines and blank lines are skipped. Lines whose first column is not all letters
 * are also skipped. Trailing whitespace on the flag is tolerated.
 */
fun parseNettalkCorpus(text: String): List<NettalkEntry> {
    val entries = mutableListOf<NettalkEntry>()
    val wordPattern = Regex("^[a-zA-Z]+$")
    for (line in text.lineSequence()) {
        val parts = line.split('\t')
        if (parts.size < 4) continue
        val word = parts[0]
        val phonemes = parts[1]
        val stress = parts[2]
        val flagStr = parts[3].trim()
        if (!wordPattern.matches(word)) continue
        if (word.length != phonemes.length || word.length != stress.length) continue
        val flag = flagStr.toIntOrNull() ?: continue
        entries.add(NettalkEntry(word.lowercase(), phonemes, stress, flag))
    }
    return entries
}

/**
 * Load the bundled NETtalk corpus.
 *
 * If [byFrequency] is `true`, loads `nettalk_by_frequency.data` — a version of the corpus
 * re-ordered by English word frequency (most common first), using the Google 20k list as
 * the ranking source. Words not present in the frequency list are appended at the end
 * in their original corpus order. Useful for training on the N most common English words.
 *
 * If `false` (the default), loads the corpus in its original alphabetical order.
 */
fun loadNettalkCorpus(byFrequency: Boolean = false): List<NettalkEntry> {
    val resource = if (byFrequency) "/nettalk/nettalk_by_frequency.data" else "/nettalk/nettalk.data"
    val stream = NettalkEntry::class.java.getResourceAsStream(resource)
        ?: error("$resource not found on classpath")
    return parseNettalkCorpus(stream.bufferedReader().readText())
}
