package org.simbrain.util

import org.simbrain.util.Utils.FS
import org.simbrain.world.textworld.TokenEmbedding
import smile.math.matrix.Matrix
import smile.nlp.tokenizer.SimpleSentenceSplitter
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

/**
 * Unique tokens: all unique tokens (contexts)
 * Converts to lowercase
 */
fun List<String>.uniqueTokensFromArray(): List<String> {
    return distinctBy { it.lowercase() }
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
 * Generally considered better for word embeddings.
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
 * Generates co-occurrence matrix from a provided [docString].
 *
 * Example: if [windowSize] is 2 and [bidirectional] is true, then the context for "dog" in "the quick [dog] ran fastly"
 * is ["the", "quick", "ran", "fastly"].  If [windowSize] 2 and [bidirectional] false, then the context for "dog"
 * is ["the", "quick"].
 *
 * @param windowSize specifies how many words should be included in a context.
 * @param bidirectional  if true, window includes this many tokens before AND after; if false the window only includes
 * previous tokens.
 * @param removeStopwords if true remove stopwords. They are removed only from rows, so that the matrix is no longer
 * square.
 * @return a co-occurrence matrix with as many rows as there are unique tokens in [docString].
 *
 */
fun generateCooccurrenceMatrix(
    docString: String,
    windowSize: Int = 2,
    bidirectional: Boolean = false,
    usePPMI: Boolean = true,
    removeStopwords: Boolean = false
): TokenEmbedding {

    if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

    var convertedDocString = docString.lowercase().removeSpecialCharacters()

    if(removeStopwords) {
        convertedDocString = convertedDocString.removeWords(stopWords)
    }

    val tokens = convertedDocString.tokenizeWordsFromString().uniqueTokensFromArray()

    // Split document into sentences
    val sentences = convertedDocString.tokenizeSentencesFromDoc()

    // Set up matrix
    val matrixSize = tokens.size
    var cocMatrix = Matrix(matrixSize, matrixSize)

    // Loop through sentences, through words
    for (sentence in sentences) {
        val tokenizedSentence = sentence.tokenizeWordsFromString()

        for (sentenceIndex in tokenizedSentence.indices) {
            val maxIndex = tokenizedSentence.size - 1  // used for window range check

            val currentToken = tokenizedSentence[sentenceIndex] // Current iterated token

            val contextLowerLimit = sentenceIndex - windowSize
            val contextUpperLimit = if (bidirectional) (sentenceIndex + windowSize) else (sentenceIndex)

            for (contextIndex in contextLowerLimit..contextUpperLimit) {
                if (contextIndex in 0..maxIndex && contextIndex != sentenceIndex) {
                    val currentContext = tokenizedSentence[contextIndex]
                    val tokenCoordinate = tokens.indexOf(currentToken)
                    val contextCoordinate = tokens.indexOf(currentContext)
                    // print(listOf("Current Token:", currentToken, tokenCoordinate))
                    // println(listOf("Current Context",currentContext, contextCoordinate))
                    cocMatrix[tokenCoordinate, contextCoordinate] =
                        cocMatrix[tokenCoordinate, contextCoordinate] + 1
                }
            }
        }
    }

    if (usePPMI) {
        cocMatrix = manualPPMI(cocMatrix, true)
    }

    return TokenEmbedding(tokens, cocMatrix.replaceNaN(0.0))
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
    val dialog = StandardDialog(null, title)
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

fun String.convertCamelCaseToSpaces(): String {
    // This regex looks for places in the string where either:
    // 1. A lowercase letter is followed by an uppercase letter, or
    // 2. A sequence of uppercase letters is followed by a lowercase letter.
    val regex = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])".toRegex()

    return regex.replace(this) { " " }
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        .trim()
}

// Test main
fun main() {
    val chooser = SFileChooser(".", "Text import", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        val text = Utils.readFileContents(theFile)
        generateCooccurrenceMatrix(text)
    }
}

/**
 * Allow file separators to be indicated with a forward slash.
 */
operator fun String.div(other: String): String {
    return this + FS + other
}