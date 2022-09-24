package org.simbrain.util

import smile.math.MathEx.cos
import smile.math.MathEx.dot
import smile.math.matrix.Matrix
import smile.nlp.tokenizer.SimpleSentenceSplitter

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
 * Unique tokens: content words only / remove stop words (targets) -- not working at the moment
 */
//fun uniqueTargetsFromArray(words: List<String>) : List<String> {
//    var stopWords = EnglishStopWords.DEFAULT
//    print("Stopwords:")
//    println(stopWords)
//    var uniqueTargets = words.distinctBy { it.lowercase() }
//    val filteredTargets = listOf<String>()
//    for (target in uniqueTargets) {
//        if (!stopWords.contains(target)) filteredTargets + target
//    }
//    return filteredTargets
//}


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
        for (indexRow in 0 until adjustedMatrix.nrows()) {
            for (indexCol in 0 until adjustedMatrix.ncols()) {
                if (adjustedMatrix[indexRow, indexCol] < 0) {
                    adjustedMatrix[indexRow, indexCol] = 0.0
                }
            }
        }
    }
    return adjustedMatrix
}

// TODO: Return a TokenVectorDictionary

/**
 * Generates co-occurrence matrix from a provided [docString].
 *
 * Example: if [windowSize] is 2 and [skipGram] is true, then the context for "dog" in "the quick dog ran fastly"
 * is ["the", "quick", "ran", "fastly"].  If [windowSize] 2 and [skipGram] false, then the context for "dog"
 * is ["the", "quick"].
 *
 * @param windowSize specifies how many words should be included in a context.
 * @param skipGram  if true, window includes this many tokens before AND after; if false the window only includes
 * previous tokens.
 * @return a symmetrical co-occurrence matrix with as many rows and columns as there are unique tokens in [docString].
 *
 */
fun generateCooccurrenceMatrix(docString: String, windowSize: Int = 2, skipGram: Boolean = false , usePPMI: Boolean = true):
        Pair<List<String>, Matrix> {
    // println(docString)
    val convertedDocString = docString.removeSpecialCharacters()

    if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

    // get tokens from whole document
    val tokenizedSentence = convertedDocString.tokenizeWordsFromSentence()
    val tokens = tokenizedSentence.uniqueTokensFromArray()

    // Split document into sentences
    val sentences = convertedDocString.tokenizeSentencesFromDoc()

    // Set up matrix
    val matrixSize = tokens.size
    val cooccurrenceSmileMatrix = Matrix(matrixSize, matrixSize)

    // cooccurrenceMatrix[0][1] = 2 // cooccurrenceMatrix[target][context]

    // Loop through sentences, through words
    for (sentence in sentences) {
        // println(sentence)
        val tokenizedSentence = sentence.tokenizeWordsFromSentence()
        for (sentenceIndex in tokenizedSentence.indices) {
            val maxIndex = tokenizedSentence.size - 1  // used for window range check

            val currentToken = tokenizedSentence[sentenceIndex] // Current iterated token

            val contextLowerLimit = sentenceIndex - windowSize
            val contextUpperLimit = if (skipGram) (sentenceIndex + windowSize) else (sentenceIndex)

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
    if (usePPMI) {
        return Pair(tokens, manualPPMI(cooccurrenceSmileMatrix, true))
    }
    return Pair(tokens, cooccurrenceSmileMatrix)
}

/**
 * Get an embedding from a matrix given matrix, index, and word
 */
fun wordEmbeddingQuery(targetWord: String, tokens: List<String>, cooccurrenceMatrix: Matrix): DoubleArray {
    val targetWordIndex = tokens.indexOf(targetWord.lowercase())
    return cooccurrenceMatrix.col(targetWordIndex)
}

/**
 * Generalized embedding similarity function.
 * The parameter [useCosine] defaults to true, so it calculates cosine similarity
 * of two vectors (higher values are more similar). If changed to false, it will
 * calculate the dot product between the two vectors.
 *
 * Cosine similarity is normalized dot product.
 *
 * All this does is forward to Math.cos but leaving it named this way is slightly more legible
 */
fun embeddingSimilarity(vectorA: DoubleArray, vectorB: DoubleArray, useCosine: Boolean = true): Double {
    return if (useCosine) {
        cos(vectorA, vectorB)
    } else dot(vectorA, vectorB)
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
