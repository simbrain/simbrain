package org.simbrain.util

import org.simbrain.util.Utils.FS
import org.simbrain.world.textworld.TokenEmbedding
import smile.math.matrix.Matrix
import smile.nlp.tokenizer.SimpleSentenceSplitter
import java.util.*
import javax.swing.JScrollPane
import javax.swing.JTextArea

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
    return this.trim().replace("[\n\r\t]".toRegex(), " ").replace("\\s+".toRegex(), " ")
}

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun String.removePunctuation(): String {
    return this.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse sentence into words.
 */
fun String.tokenizeWordsFromSentence(): List<String> {
    return this.lowercase().removePunctuation().split(" ")
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
    val stopWords = ResourceManager
        .readFileContents("textworld" + FS + "stopwords.txt")
        .split("\n").toSet()
    val uniqueTargets = words.distinctBy { it.lowercase() }
    val filteredTargets = mutableListOf<String>()
    for (target in uniqueTargets) {
        if (!stopWords.contains(target)) filteredTargets += listOf(target)
    }
    return filteredTargets
}

/**
 * Removes stopwords from provided list of tokens, and then filters the rows by the resulting "meaningful" word list
 *
 * The resulting matrix has the same number of columns it started with, but only as many rows as there are after
 * filtering out stopwords.
 */
fun removeStopWordsFromMatrix(cocMatrix: Matrix, tokens: List<String>) : TokenEmbedding {
    val targets = removeStopWords(tokens)
    var approvedIndices = intArrayOf()
    for (token in tokens){
        if (targets.contains(token)) approvedIndices += intArrayOf(tokens.indexOf(token))
    }

    return TokenEmbedding(targets, cocMatrix.rows(*approvedIndices))
}


// After writing, found out that SimBrain already has an outerProduct function.
fun outerProduct(vectorU: DoubleArray, vectorV: DoubleArray): Matrix {
    // u (*) v = [u1 ... ui].vertical * [v1 ... vj].horizontal
    val rows = vectorU.size
    val cols = vectorV.size
    val outerProductMatrix = Matrix(rows, cols)
    for (indexU in vectorU.indices) for (indexV in vectorV.indices) outerProductMatrix[indexU, indexV] =
        vectorU[indexU] * vectorV[indexV]
    return outerProductMatrix
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

    val expectedValues = outerProduct(rowTotals, columnTotals) / totalSum
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
 * @return a symmetrical co-occurrence matrix with as many rows and columns as there are unique tokens in [docString].
 *
 */
fun generateCooccurrenceMatrix(docString: String, windowSize: Int = 2, bidirectional: Boolean = false, usePPMI:
Boolean = true, removeStopwords: Boolean = true):
        TokenEmbedding {
    // println(docString)
    val convertedDocString = docString.removeSpecialCharacters()

    if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

    // get tokens from whole document
    val tokenizedSentence = convertedDocString.tokenizeWordsFromSentence()
    val tokens = tokenizedSentence.uniqueTokensFromArray()
    val targets = removeStopWords(tokens)

    // Split document into sentences
    val sentences = convertedDocString.tokenizeSentencesFromDoc()

    // Set up matrix
    val matrixSize = tokens.size
    var cooccurrenceSmileMatrix = Matrix(matrixSize, matrixSize)

    // cooccurrenceMatrix[0][1] = 2 // cooccurrenceMatrix[target][context]

    // Loop through sentences, through words
    for (sentence in sentences) {
        // println(sentence)
        val tokenizedSentence = sentence.tokenizeWordsFromSentence()
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
                    cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] =
                        cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] + 1
                }
            }


        }
    }

    if (removeStopwords){
        if (usePPMI) {
            return removeStopWordsFromMatrix(manualPPMI(cooccurrenceSmileMatrix, true).replaceNaN(0.0), tokens)
        } else {
            return removeStopWordsFromMatrix(cooccurrenceSmileMatrix.replaceNaN(0.0), tokens)
        }
    }

    if (usePPMI) {
        return TokenEmbedding(tokens, manualPPMI(cooccurrenceSmileMatrix, true).replaceNaN(0.0))
    }
    return TokenEmbedding(tokens, cooccurrenceSmileMatrix.replaceNaN(0.0))
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
    dialog.addClosingTask {
        commitAction(textArea.text)
    }

    return dialog
}

fun String.convertCamelCaseToSpaces(): String {
    val regex = "(?<=\\w)([A-Z])".toRegex()
    return regex.replace(this) { " ${it.value}" }
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
