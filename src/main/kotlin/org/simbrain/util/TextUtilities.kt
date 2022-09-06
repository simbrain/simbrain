package org.simbrain.util

import smile.math.MathEx.cos
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
    return this.trim().replace("[\n\r\t]".toRegex(), " ")
}

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun removePunctuation(str: String): String {
    return str.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse sentence into words.
 */
fun tokenizeWordsFromSentence(sentence: String): List<String> {
    return removePunctuation(sentence.lowercase()).split(" ")
}

/**
 * Unique tokens: all unique tokens (contexts)
 * Converts to lowercase
 */
fun uniqueTokensFromArray(words: List<String>): List<String> {
    return words.distinctBy { it.lowercase() }
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
        for (indexRow in 0..(adjustedMatrix.nrows() - 1)) {
            for (indexCol in 0..(adjustedMatrix.ncols() - 1)) {
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
 * Generates co-occurrence matrix from a provided [docString]. [windowSize] specifies how many words should be
 * included in a context.
 *
 * Example: if [windowSize] 2, then the context for "dog" in "the quick dog ran fastly" is ["the", "dog", "ran",
 * "fastly'].
 *
 * Returns a symmetrical co-occurrence matrix with as many rows and columns as there are unique tokens in [docString].
 *
 */
fun generateCooccurrenceMatrix(docString: String, windowSize: Int = 2, usePPMI: Boolean = true):
        Pair<List<String>, Matrix> {
    // println(docString)
    val convertedDocString = docString.removeSpecialCharacters()

    if (windowSize == 0) throw IllegalArgumentException("windowsize must be greater than 0")

    // get tokens from whole document
    val tokenizedSentence = tokenizeWordsFromSentence(convertedDocString)
    // print("Tokenized sentence: ")
    // println(tokenizedSentence)

    val tokens = uniqueTokensFromArray(tokenizedSentence)
    // print("Tokens: ")
    // println(tokens)

    // Split document into sentences
    val sentences = convertedDocString.tokenizeSentencesFromDoc()
    // print("Sentences: ")
    // println(sentences)

    // Set up matrix
    val matrixSize = tokens.size
    val cooccurrenceSmileMatrix = Matrix(matrixSize, matrixSize)

    // cooccurrenceMatrix[0][1] = 2 // cooccurrenceMatrix[target][context]

    // Loop through sentences, through words
    for (sentence in sentences) {
        // println(sentence)
        val tokenizedSentence = tokenizeWordsFromSentence(sentence)
        for (sentenceIndex in tokenizedSentence.indices) {
            val maxIndex = tokenizedSentence.size - 1  // used for window range check

            val currentToken = tokenizedSentence[sentenceIndex] // Current iterated token

            val contextLowerLimit = sentenceIndex - windowSize
            val contextUpperLimit = sentenceIndex + windowSize

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
 * Calculate cosine similarity of two vectors (higher values are more similar).
 * All this does is forward to Math.cos but leaving it named this way is slightly more legible
 */
fun cosineSimilarity(vectorA: DoubleArray, vectorB: DoubleArray): Double {
    return cos(vectorA, vectorB)
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
