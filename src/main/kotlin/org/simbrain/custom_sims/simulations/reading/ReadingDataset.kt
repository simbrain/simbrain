/** Small, inspectable word data and sampling support for the triangle reading simulation. */
package org.simbrain.custom_sims.simulations.reading

import kotlin.random.Random

enum class Regularity { REGULAR, EXCEPTION }

enum class SamplingMode { FREQUENCY_WEIGHTED, UNIFORM }

data class ReadingWord(
    val word: String,
    val pronunciation: List<String>,
    val frequency: Int,
    val regularity: Regularity
) {
    val frequencyBand: String get() = if (frequency >= 50) "High" else "Low"

    override fun toString() = word
}

class ReadingDataset(val words: List<ReadingWord>) {
    init {
        require(words.isNotEmpty()) { "A reading dataset must contain at least one word" }
        require(words.map { it.word }.distinct().size == words.size) { "Words must be unique" }
        require(words.all { it.frequency > 0 }) { "Frequencies must be positive" }
    }

    companion object {
        fun loadDevelopmentDataset(): ReadingDataset {
            val stream = ReadingDataset::class.java.getResourceAsStream("/reading/triangle_words.tsv")
                ?: error("Bundled reading dataset not found")
            val rows = stream.bufferedReader().useLines { lines ->
                lines.drop(1).filter { it.isNotBlank() }.map { line ->
                    val fields = line.split('\t')
                    require(fields.size == 4) { "Expected four columns in '$line'" }
                    ReadingWord(
                        word = fields[0].lowercase(),
                        pronunciation = fields[1].split(' '),
                        frequency = fields[2].toInt(),
                        regularity = Regularity.valueOf(fields[3].uppercase())
                    )
                }.toList()
            }
            return ReadingDataset(rows)
        }
    }
}

class WordSampler(private val dataset: ReadingDataset, seed: Int = 42) {
    private var random = Random(seed)

    fun reset(seed: Int) {
        random = Random(seed)
    }

    fun nextIndex(mode: SamplingMode): Int = when (mode) {
        SamplingMode.UNIFORM -> random.nextInt(dataset.words.size)
        SamplingMode.FREQUENCY_WEIGHTED -> {
            val draw = random.nextDouble(dataset.words.sumOf { it.frequency }.toDouble())
            var total = 0.0
            dataset.words.indexOfFirst { word ->
                total += word.frequency
                draw < total
            }.coerceAtLeast(0)
        }
    }
}
