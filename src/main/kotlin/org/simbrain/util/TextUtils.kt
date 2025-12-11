package org.simbrain.util

import org.simbrain.util.Utils.FS
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import smile.math.matrix.Matrix
import smile.nlp.tokenizer.SimpleSentenceSplitter
import java.awt.Frame
import java.util.*
import javax.swing.JScrollPane
import javax.swing.JTextArea

val stopWords by lazy {
    ResourceManager
        .readFileContents("textworld" + FS + "stopwords.txt")
        .split("\n").toSet().toList()
}

/**
 * Sentence tokenizer: parse document into sentences and return as a list of sentences.
 *
 * Forward to Smile's sentence splitter.
 */
fun String.tokenizeSentencesFromDoc(): List<String> {
    return SimpleSentenceSplitter.getInstance().split(this).toList()
}

/**
 * Trims extra whitespace and removes newlines, returns, and tabs.
 */
fun String.removeSpecialCharacters(): String {
    return this.trim().normalizeSpacing()
}

/**
 * Removes extra spaces and newlines
 */
fun String.normalizeSpacing(): String = replace("[\n\r\t]".toRegex(), " ").replace("\\s+".toRegex(), " ")

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun String.removePunctuation(): String {
    return this.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse string into words.
 */
fun String.tokenizeWordsFromString(): List<String> {
    return this.lowercase().removeSpecialCharacters().removePunctuation().split(" ")
}

abstract class Tokenizer<T: CopyableObject>: CopyableObject {
    open fun initialize(trainingDocument: String) {}
    abstract fun tokenize(text: String): List<TokenizerResult>
    abstract fun joinTokens(tokens: List<String>): String

    override fun getTypeList() = listOf(SimpleTokenizer::class.java, BytePairTokenizer::class.java)
}

class SimpleTokenizer(
    usePunctuation: Boolean = false,
    useSpaces: Boolean = false,
    useReturns: Boolean = false,
): Tokenizer<SimpleTokenizer>() {

    var usePunctuation by GuiEditable(
        initValue = usePunctuation,
        description = "If true, keep punctuation marks and add them as tokens",
        order = 10
    )

    var useSpaces by GuiEditable(
        initValue = useSpaces,
        description = "If true, use spaces, tabs, and newlines as distinct tokens",
        order = 20
    )

    var useReturns by GuiEditable(
        initValue = useReturns,
        description = "If true, use newlines as tokens",
        order = 30
    )

    override fun tokenize(text: String): List<TokenizerResult> {
        val regex = listOfNotNull(
            """[\w'’]+""", // Words, including words like don't with apostrophes (both straight and curly)
            if (useSpaces) """\s""" else null,
            if (useReturns) """\n""" else null,
            if (usePunctuation) """[^\w\s]""" else null
        ).joinToString("|").toRegex()

        return regex.findAll(text).map { TokenizerResult(it.value.lowercase(), it.range.first, it.range.last) }.toList()
    }

    /**
     * Join a set of tokens in a way that follows standard spacing conventions:
     *   - Spaces between words.
     *   - Spaces after punctuation but not before punctuation.
     *   - No spaces before or after returns.
     *
     * If useSpaces = true, these rules are not used, all tokens are joined without spaces because the space is itself a token
     */
    override fun joinTokens(tokens: List<String>): String {
        val punctuationRegex = """\W""".toRegex()
        val returnsRegex = """\n""".toRegex()
        if (tokens.size <= 1) return tokens.joinToString("")
        return tokens.windowed(2)
            .mapIndexed { i, pair ->
                val secondTokenIsPunctuation = punctuationRegex.matches(pair[1])
                val eitherTokenIsReturn = returnsRegex.matches(pair[0]) || returnsRegex.matches(pair[1])
                val shouldAddSpace = !secondTokenIsPunctuation && !eitherTokenIsReturn && !useSpaces
                if (i == 0) {
                    listOf(pair[0], if (shouldAddSpace) " " else "", pair[1])
                } else {
                    listOf(if (shouldAddSpace) " " else "", pair[1])
                }
            }
            .flatten()
            .joinToString("")
    }

    override fun copy(): SimpleTokenizer {
        return SimpleTokenizer(
            usePunctuation = this.usePunctuation,
            useSpaces = this.useSpaces,
            useReturns = this.useReturns
        )
    }

}

// The Byte Pair Tokenizer with BPE training and tokenization.
class BytePairTokenizer(
    maxTokens: Int = 1000,
    minFrequency: Int = 2,
    maxIterations: Int = 100,
) : Tokenizer<BytePairTokenizer>() {

    // These instance variables are made “GUI editable” in your environment.
    var maxTokens by GuiEditable(
        initValue = maxTokens,
        description = "Maximum number of tokens to generate",
        order = 10
    )
    var minFrequency by GuiEditable(
        initValue = minFrequency,
        description = "Minimum frequency of tokens to keep",
        order = 20
    )
    var maxIterations by GuiEditable(
        initValue = maxIterations,
        description = "Maximum number of iterations to run",
        order = 30
    )

    // The learned merge rules.
    // Each key is a pair of strings and its value is the order (priority) at which it was merged.
    // A lower integer means the pair was merged earlier.
    private var mergeRules: MutableMap<Pair<String, String>, Int> = mutableMapOf()

    // For training, we maintain a “vocabulary” of training samples.
    // In this version the entire sample text is treated as one sequence of tokens.
    // Each key is a list of tokens (initially single characters) and its value is its frequency.
    private var bpeVocabulary: MutableMap<List<String>, Int> = mutableMapOf()

    /**
     * Trains the BPE merge rules from a sample text.
     *
     * The training algorithm is:
     *  1. Represent the entire sample text as a list of characters (tokens).
     *  2. Count adjacent token pairs over the sample.
     *  3. Pick the most frequent pair (with frequency >= minFrequency) and add it to mergeRules.
     *  4. Replace occurrences of that pair in the token list.
     *  5. Repeat until no eligible pairs remain, maxIterations is reached, or we reached maxTokens.
     */
    override fun initialize(trainingDocument: String) {
        // Reset learned merges and vocabulary.
        reset()

        // Treat the entire sample text as one training sample,
        // so that every character (including whitespace and punctuation) is considered.
        val tokens = trainingDocument.map { it.toString() }
        bpeVocabulary[tokens] = 1  // Since it's one sample, its frequency is 1.

        var currentIteration = 0
        while (currentIteration < maxIterations) {
            // Count frequency of all adjacent pairs in the vocabulary.
            val pairFrequencies = mutableMapOf<Pair<String, String>, Int>()
            for ((tokenList, freq) in bpeVocabulary) {
                if (tokenList.size < 2) continue
                for (i in 0 until tokenList.size - 1) {
                    val pair = Pair(tokenList[i], tokenList[i + 1])
                    pairFrequencies[pair] = pairFrequencies.getOrDefault(pair, 0) + freq
                }
            }

            // Find the most frequent pair that meets the minFrequency requirement.
            val candidatePair = pairFrequencies
                .filter { it.value >= minFrequency }
                .maxByOrNull { it.value }?.key

            // If no candidate pair is found, or we already have enough merges, break.
            if (candidatePair == null || mergeRules.size >= maxTokens) break

            // Add the candidate pair to our merge rules with the current iteration as its order.
            mergeRules[candidatePair] = currentIteration

            // Update the vocabulary by merging this candidate pair in the token list.
            val newVocabulary = mutableMapOf<List<String>, Int>()
            for ((tokenList, freq) in bpeVocabulary) {
                val newTokens = mergeTokens(tokenList, candidatePair)
                newVocabulary[newTokens] = newVocabulary.getOrDefault(newTokens, 0) + freq
            }
            bpeVocabulary = newVocabulary

            currentIteration++
        }
    }

    /**
     * Resets the learned merge rules and vocabulary.
     */
    fun reset() {
        mergeRules.clear()
        bpeVocabulary.clear()
    }

    /**
     * Tokenizes a given text using the learned BPE merge rules.
     *
     * In this version, the entire input text (including whitespaces and punctuation)
     * is processed as a single sequence.
     *
     * For each token, its start and end offsets in the original string are recorded.
     */
    override fun tokenize(text: String): List<TokenizerResult> {

        // Apply BPE merges to the entire text.
        val tokens = bpeEncode(text)
        val results = mutableListOf<TokenizerResult>()
        var offset = 0
        for (token in tokens) {
            results.add(TokenizerResult(token, offset, offset + token.length))
            offset += token.length
        }
        return results
    }

    /**
     * Reconstructs text by joining tokens without additional spaces.
     */
    override fun joinTokens(tokens: List<String>): String {
        return tokens.joinToString("")
    }

    /**
     * Applies the learned BPE merge rules to a given string.
     *
     * The algorithm:
     *  - Start with the list of characters in the input string.
     *  - Repeatedly check for any adjacent pair that exists in mergeRules.
     *  - If found, merge the pair with the lowest (i.e. highest priority) order.
     *  - Stop when no more merges can be applied.
     */
    private fun bpeEncode(text: String): List<String> {
        // Start with the characters of the input text.
        var tokens = text.map { it.toString() }
        if (mergeRules.isEmpty()) return tokens

        // Continue merging until no applicable pairs remain.
        while (true) {
            // Build a list of candidate pairs (with their index and merge order).
            val candidates = mutableListOf<Triple<Int, Int, Pair<String, String>>>()
            for (i in 0 until tokens.size - 1) {
                val pair = Pair(tokens[i], tokens[i + 1])
                mergeRules[pair]?.let { order ->
                    candidates.add(Triple(i, order, pair))
                }
            }
            if (candidates.isEmpty()) break

            // Choose the candidate with the lowest merge order (merged earliest during training).
            val (index, _, pair) = candidates.minByOrNull { it.second }!!
            tokens = mergePairAt(tokens, index)
        }
        return tokens
    }

    /**
     * Merges the pair at the given index (i.e. tokens[index] and tokens[index+1]).
     */
    private fun mergePairAt(tokens: List<String>, index: Int): List<String> {
        return tokens.take(index) +
                listOf(tokens[index] + tokens[index + 1]) +
                tokens.drop(index + 2)
    }

    /**
     * Merges all occurrences of a given pair in a list of tokens.
     *
     * For example, if the pair is ("a", "b"), then ["a", "b", "a"] becomes ["ab", "a"].
     */
    private fun mergeTokens(tokens: List<String>, targetPair: Pair<String, String>): List<String> {
        val merged = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            if (i < tokens.size - 1 && tokens[i] == targetPair.first && tokens[i + 1] == targetPair.second) {
                merged.add(tokens[i] + tokens[i + 1])
                i += 2
            } else {
                merged.add(tokens[i])
                i++
            }
        }
        return merged
    }

    override fun copy(): BytePairTokenizer {
        return BytePairTokenizer(
            maxTokens = maxTokens,
            minFrequency = minFrequency,
            maxIterations = maxIterations
        )
    }
}

fun String.tokenize(tokenizer: Tokenizer<*>): List<TokenizerResult> = tokenizer.tokenize(this)
fun List<String>.tokensToString(tokenizer: Tokenizer<*>) = tokenizer.joinTokens(this)

class TokenizerResult(
    val token: String,
    val start: Int,
    val end: Int
)

/**
 * Returns a sorted list of unique tokens in a list of tokens, converted to lowercase.
 */
fun List<String>.uniqueTokensFromArray(): List<String> {
    return distinctBy { it.lowercase() }.sorted()
}

/**
 * Removes stop words (a word with little bearing on meaning; e.g. grammatical words like "from" or "an")
 * from a provided list of strings
 *
 * Stopwords obtained from https://gist.github.com/larsyencken/1440509#file-stopwords-txt
 */
fun removeStopWords(words: List<String>) : List<String> {
    return words.distinctBy { it.lowercase() }.filter { !stopWords.contains(it) }
}

/**
 * Positive Pointwise Mutual Information weighting
 * Adapted from: https://stackoverflow.com/questions/58701337/how-to-construct-ppmi-matrix-from-a-text-corpus
 *
 * Weights the co-occurrence values to avoid word-frequency-bias in embeddings. Words like "the" and "a" that should
 * not be considered meaningful in terms of co-occurrence are down-weighted. Less frequent words on the other, like
 * "platitude" or "espresso", that are more meaningful in terms of co-occurrences, are up-weighted.
 *
 * Generally considered better for token embeddings.
 *
 * "PPMI measures how much the probability of a target–context pair estimated in the training corpus is higher than
 * the probability we should expect if the target and the context occurred independently of one another." (Lenci, 2018)
 *
 * @param positive if true, changes negative adjusted co-occurrence values to 0
 */
fun manualPPMI(cocMatrix: Matrix, positive: Boolean = true): Matrix {
    val columnTotals = cocMatrix.colSums()
    val totalSum = columnTotals.sum()
    val rowTotals = cocMatrix.rowSums()

    val expectedValues = rowTotals.outerProduct(columnTotals) / totalSum
    val adjustedMatrix = cocMatrix.clone().div(expectedValues)

    if (positive) {
        for (indexRow in 0 until adjustedMatrix.nrow()) {
            for (indexCol in 0 until adjustedMatrix.ncol()) {
                if (adjustedMatrix[indexRow, indexCol] < 0) {
                    adjustedMatrix[indexRow, indexCol] = 0.0
                }
            }
        }
    }
    return adjustedMatrix
}

fun String.removeWords(wordsToRemove: List<String>): String {
    var result = this
    for (word in wordsToRemove) {
        result = result.replace("\\b$word\\b".toRegex(), "") // using word boundaries to match whole words
    }
    return result.trim().normalizeSpacing()
}

/**
 * Generalized embedding similarity function.
 * The parameter [useCosine] defaults to true, so it calculates cosine similarity
 * of two vectors (higher values are more similar). If changed to false, it will
 * calculate the dot product between the two vectors.
 *
 * Cosine similarity is normalized dot product.
 */
fun embeddingSimilarity(vectorA: DoubleArray, vectorB: DoubleArray, simfun: (DoubleArray, DoubleArray) -> Double =
    smile.math.MathEx::cos): Double {
    return simfun(vectorA, vectorB)
}

@JvmOverloads
fun textEntryDialog(initialString: String, title: String = "Edit Text", columns: Int = 20, rows: Int = 5, commitAction: (String) -> Unit = {}): StandardDialog {
    val dialog = StandardDialog(null as Frame?, title)
    val textArea = JTextArea(initialString)
    textArea.columns = columns
    textArea.rows = rows

    val scrollPane = JScrollPane(textArea)
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    dialog.contentPane = scrollPane

    textArea.requestFocusInWindow()
    dialog.addCommitTask {
        commitAction(textArea.text)
    }

    return dialog
}

/**
 * Converts a camelCase or PascalCase string to a human-readable format with spaces.
 * 
 * @param useTitleCase If true, capitalizes the first letter of each word (Title Case).
 *                     If false (default), only capitalizes the first letter (Sentence case).
 * @return The converted string with spaces and appropriate capitalization.
 * 
 * Examples:
 * - "helloWorld".convertCamelCaseToSpaces() -> "Hello world"
 * - "helloWorld".convertCamelCaseToSpaces(useTitleCase = true) -> "Hello World"
 * - "XMLHttpRequest".convertCamelCaseToSpaces() -> "XML http request"
 * - "XMLHttpRequest".convertCamelCaseToSpaces(useTitleCase = true) -> "XML Http Request"
 */
fun String.convertCamelCaseToSpaces(useTitleCase: Boolean = false): String {
    // This regex looks for places in the string where either:
    // 1. A lowercase letter is followed by an uppercase letter, or
    // 2. A sequence of uppercase letters is followed by a lowercase letter.
    val regex = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])".toRegex()

    val withSpaces = regex.replace(this) { " " }.trim()
    
    return if (useTitleCase) {
        // Title case: capitalize first letter of each word
        withSpaces.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } else {
        // Sentence case: lowercase everything except the first character
        withSpaces.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}

/**
 * Generates a "Christmas tree" of input-target pairs in an autoregressive manner from a given string.
 * Each pair consists of a prefix of tokens as input and the next token as the target.
 *
 * See test case for a clear demo of how it works.
 */
fun generateAutoregressivePairs(context: List<String>): List<Pair<List<String>, String>> = buildList {
    for (i in 0 until context.size - 1) {
        add(context.subList(0, i + 1) to context[i + 1])
    }
}

fun String.indent(indentation: Int = 2): String {
    return split("\n").joinToString("\n") { " ".repeat(indentation) + it }
}

// Test main
fun main() {
    val chooser = SFileChooser(".", "Text import", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        val text = Utils.readFileContents(theFile)
        TokenEmbeddingBuilder().apply {
            tokenizer = SimpleTokenizer()
            embeddingType = EmbeddingType.CoOccurrence()
        }.build(text)
    }
}

/**
 * Allow file separators to be indicated with a forward slash.
 */
operator fun String.div(other: String): String {
    return this + FS + other
}