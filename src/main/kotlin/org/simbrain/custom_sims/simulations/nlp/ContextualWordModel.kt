/**
 * A headless [TinyLmModel] trained on one-sentence samples, either as a masked language model
 * (BERT-style: bidirectional attention, predict hidden words) or as a next-word model (GPT-style:
 * causal attention, predict the following word). Exposes the two kinds of word vector the
 * contextual-meaning simulation compares: a word's fixed row in the input embedding table, and
 * the model's output vector for that word inside a particular sentence.
 */
package org.simbrain.custom_sims.simulations.nlp

import org.simbrain.custom_sims.simulations.nlp.ContextualWordModel.Companion.MASK
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.network.llm.TinyLmModel
import kotlin.random.Random

class ContextualWordModel(
    sentences: List<String>,
    val masked: Boolean,
    embedDim: Int = 16,
    numHeads: Int = 2,
    numLayers: Int = 2,
    hiddenDim: Int = 32,
    learningRate: Float = 0.003f,
    seed: Long = 1L,
) {

    private val samples: List<List<String>> = sentences.map(::words).filter { it.isNotEmpty() }

    val vocabulary: List<String> = listOf(PAD, MASK) + samples.flatten().distinct().sorted()

    private val index = vocabulary.withIndex().associate { (i, w) -> w to i }

    val contextSize = samples.maxOf { it.size }

    val model = TinyLmModel(
        TinyLmConfig(
            contextSize = contextSize,
            embedDim = embedDim,
            numHeads = numHeads,
            hiddenDim = hiddenDim,
            vocabSize = vocabulary.size,
            numLayers = numLayers,
            bidirectional = masked,
        ),
        seed,
    ).apply { adam.learningRate = learningRate }

    private val rng = Random(seed)

    /**
     * Fraction of words chosen for prediction in each training sentence (masked mode only). As in BERT, a
     * chosen word becomes `[mask]` 80% of the time, a random word 10%, and stays unchanged 10%; the model
     * must predict the original either way, which teaches it to reproduce words it can see.
     */
    var maskRate = 0.15

    /** Training sentences seen so far, in units of full passes through the training text. */
    val epochs: Double get() = sentencesSeen.toDouble() / samples.size

    private var sentencesSeen = 0

    private val queue = ArrayDeque<List<String>>()

    /** The vocabulary minus the padding and mask markers: the words the model can usefully predict. */
    val predictableWords: List<String> = vocabulary.drop(2)

    /** What the model saw and produced on its most recent training sentence, masks included. */
    @Volatile
    var lastTrained: Snapshot? = null
        private set

    fun knows(word: String) = word in index

    private fun ids(words: List<String>) = IntArray(contextSize) { i ->
        words.getOrNull(i)?.let { index[it] ?: error("Unknown word '$it'") } ?: index.getValue(PAD)
    }

    /**
     * Trains on the next [count] sentences, reshuffling the training text each time it runs out;
     * returns the mean loss.
     */
    fun trainSentences(count: Int): Double = synchronized(model) {
        var total = 0.0
        repeat(count) {
            if (queue.isEmpty()) queue.addAll(samples.shuffled(rng))
            val sentence = queue.removeFirst()
            val (input, target) = if (masked) maskedSample(sentence) else nextWordSample(sentence)
            total += model.trainStep(input, target)
            sentencesSeen++
            if (it == count - 1) {
                val shown = input.take(sentence.size).map { id -> vocabulary[id] }
                val chosen = if (masked) target.indices.filter { i -> target[i] >= 0 }.toSet() else emptySet()
                lastTrained = capture(shown, chosen.minOrNull() ?: 0, chosen)
            }
        }
        total / count
    }

    private fun maskedSample(sentence: List<String>): Pair<IntArray, IntArray> {
        val input = ids(sentence)
        val target = IntArray(contextSize) { -1 }
        val chosen = sentence.indices.filter { rng.nextDouble() < maskRate }
            .ifEmpty { listOf(sentence.indices.random(rng)) }
        for (i in chosen) {
            target[i] = input[i]
            val roll = rng.nextDouble()
            input[i] = when {
                roll < 0.8 -> index.getValue(MASK)
                roll < 0.9 -> rng.nextInt(2, vocabulary.size)
                else -> input[i]
            }
        }
        return input to target
    }

    private fun nextWordSample(sentence: List<String>): Pair<IntArray, IntArray> {
        val input = ids(sentence)
        val target = IntArray(contextSize) { i -> if (i + 1 < sentence.size) input[i + 1] else -1 }
        return input to target
    }

    /** The word's fixed vector: its row in the input embedding table, the same in every sentence. */
    fun wordVector(word: String): DoubleArray = synchronized(model) {
        val table = model.params.getValue("embed.table").tensor
        val row = index.getValue(word)
        DoubleArray(table.cols) { table[row, it].toDouble() }
    }

    /**
     * The model's output vector for the word at [position] of [sentence]: the final normalized
     * residual stream at that position, which has mixed in whatever context attention allows.
     */
    fun contextualVector(sentence: String, position: Int): DoubleArray =
        run(words(sentence), position).outputVectors[position]

    /**
     * Runs [inputWords] through the model and records the result. [focus] is the position whose output
     * vector gets plotted; [hidden] marks the guesses that are for hidden words (by default every `[mask]`).
     */
    fun run(
        inputWords: List<String>,
        focus: Int,
        hidden: Set<Int> = inputWords.indices.filter { inputWords[it] == MASK }.toSet(),
    ): Snapshot = synchronized(model) {
        model.setSample(ids(inputWords))
        model.forward()
        capture(inputWords, focus, hidden)
    }

    /**
     * Runs a sentence with one word replaced by [MASK]. A masked model sees the whole sentence; a
     * next-word model sees only the words before the mask and predicts from the last of them.
     */
    fun fillIn(sentenceWords: List<String>): Snapshot {
        val blank = sentenceWords.indexOf(MASK)
        return if (masked || blank == 0) run(sentenceWords, blank)
        else run(sentenceWords.take(blank), blank - 1, hidden = setOf(blank - 1))
    }

    /** Reads the input and output vectors and each word's top guess from the forward pass just run. */
    private fun capture(inputWords: List<String>, focus: Int, hidden: Set<Int>): Snapshot {
        val table = model.params.getValue("embed.table").tensor
        val out = model.plan.port("final_normed").tensor
        val padded = List(contextSize) { inputWords.getOrNull(it) ?: PAD }
        val guesses = padded.indices.map { position ->
            if (padded[position] == PAD) return@map null
            val probabilities = model.distributionAt(position)
            predictableWords.indices.maxBy { probabilities[it + 2] }
                .let { predictableWords[it] to probabilities[it + 2].toDouble() }
        }
        return Snapshot(
            words = padded,
            inputVectors = padded.map { w -> DoubleArray(table.cols) { table[index.getValue(w), it].toDouble() } },
            outputVectors = List(contextSize) { r -> DoubleArray(out.cols) { out[r, it].toDouble() } },
            guesses = guesses,
            focus = focus,
            hidden = hidden,
        )
    }

    /**
     * One pass through the model: the words in each position (padded to the context size), each word's
     * fixed input vector, the output vector at each position, and the top guess (word and probability)
     * at each non-padding position. A masked model guesses the word at that position; a next-word model
     * guesses the word after it. [hidden] marks the guesses that are for hidden words, and [focus] the
     * position whose output vector is plotted.
     */
    inner class Snapshot(
        val words: List<String>,
        val inputVectors: List<DoubleArray>,
        val outputVectors: List<DoubleArray>,
        val guesses: List<Pair<String, Double>?>,
        val focus: Int,
        val hidden: Set<Int>,
    ) {
        /** Whether this comes from a masked model. */
        val masked: Boolean get() = this@ContextualWordModel.masked
    }

    companion object {
        const val PAD = "[pad]"
        const val MASK = "[mask]"

        fun words(sentence: String) = sentence.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    }
}
