package org.simbrain.util

import smile.math.MathEx.cos
import smile.nlp.dictionary.EnglishStopWords
import smile.nlp.tokenizer.SimpleSentenceSplitter
import smile.math.matrix.Matrix

/**
 * Sentence tokenizer: parse document into sentences and return as a list of sentences.
 *
 * Forward to Smile's sentence splitter.
 */
fun tokenizeSentencesFromDoc(docString: String) : List<String> {
    return  SimpleSentenceSplitter.getInstance().split(docString.lowercase()).toList()
}

/**
 * https://www.techiedelight.com/remove-punctuation-from-a-string-in-kotlin/
 */
fun removePunctuation(str: String) : String {
    return str.replace("\\p{Punct}".toRegex(), "");
}

/**
 * Word tokenizer: parse sentence into words.
 */
fun tokenizeWordsFromSentence(sentence: String) : List<String> {
    return removePunctuation(sentence.lowercase()).split(" ")
}

/**
 * Unique tokens: all unique tokens (contexts)
 * Converts to lowercase
 */
fun uniqueTokensFromArray(words: List<String>) : List<String> {
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



/**
 * Generates co-occ matrix w/ window size n
 */
fun generateCooccurrenceMatrix(docString: String, windowSize: Int): Matrix  {
    // get tokens from whole document
    var tokenizedSentence = tokenizeWordsFromSentence(docString)
    var tokens = uniqueTokensFromArray(tokenizedSentence)
    println("Tokens:")
    println(tokens)

    // Split document into sentences
    var sentences = tokenizeSentencesFromDoc(docString)

    // Set up matrix
    var matrixSize = tokens.size
    var cooccurrenceSmileMatrix = Matrix(matrixSize, matrixSize)

//    cooccurrenceMatrix[0][1] = 2 // cooccurrenceMatrix[target][context]

    // Loop through sentences, through words
    for (sentence in sentences) {
        println(sentence)
        var tokenizedSentence = tokenizeWordsFromSentence(sentence)
        for (sentenceIndex in tokenizedSentence.indices) {
            var maxIndex = tokenizedSentence.size -1  // used for window range check

            var currentToken = tokenizedSentence[sentenceIndex] // Current iterated token

            var contextLowerLimit = sentenceIndex - windowSize
            var contextUpperLimit = sentenceIndex + windowSize

            for (contextIndex in contextLowerLimit..contextUpperLimit){
                if (contextIndex in 0..maxIndex && contextIndex != sentenceIndex){
                    var currentContext = tokenizedSentence[contextIndex]


                    var tokenCoordinate = tokens.indexOf(currentToken)
                    var contextCoordinate = tokens.indexOf(currentContext)
//                    print(listOf("Current Token:", currentToken, tokenCoordinate))
//                    println(listOf("Current Context",currentContext, contextCoordinate))
                    cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] = cooccurrenceSmileMatrix[tokenCoordinate, contextCoordinate] + 1
                }
            }
        }
    }
    print(cooccurrenceSmileMatrix)
    return cooccurrenceSmileMatrix
}

/**
 * Get an embedding from a matrix given matrix, index, and word
 */
//fun generateCooccurrenceMatrix(docString: String, windowSize: Int): Matrix  {
fun wordEmbeddingQuery(targetWord: String, tokens: List<String>, cooccurrenceMatrix: Matrix): DoubleArray {
    var targetWordIndex = tokens.indexOf(targetWord)
    return cooccurrenceMatrix.col(targetWordIndex)

}


/**
 * PPMI weighting
 * Adjusted from:  https://stackoverflow.com/questions/58701337/how-to-construct-ppmi-matrix-from-a-text-corpus
 * https://haifengl.github.io/api/java/smile/math/matrix/Matrix.html#colSums--
 */
//fun manualPPMI(cooccurrenceMatrix: Matrix, positive: Boolean): Matrix {
//    // Get vector of column totals
//    var columnTotals = colSums(cooccurrenceMatrix)
//    // Get total sum of cooccurrences
//    var totalSum = sum(columnTotals)
//    // Get vector of row totals
//    var rowTotals = rowSums(cooccurrenceMatrix)
//    // "expected values" as the outer product of (row totals, col totals) / total
//
//    // Divide cooccurrence matrix by the expected values
//
//    // If positive, then fill in negatives with zero
//}

/**
 * Calculate cosine similarity of two vectors (higher values are more similar)
 */
fun embeddingCosineSimilarity(vectorA: DoubleArray, vectorB: DoubleArray): Double {
    return cos(vectorA, vectorB)

}


