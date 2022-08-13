package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextUtilsTest {

    val simpleText = "This is a simple sentence. This is not hard."

    val similarText = "The cat can run. The dog can run. The cat eats food. The dog eats food. Please bring lunch to the table."

    val harderText = "In spite of these three obstacles, the fragmentary Don Quixote of Menard is more subtle than that of Cervantes. The latter indulges in a rather coarse opposition between tales of knighthood and the meager, provincial reality of his country; Menard chooses as 'reality' the land of Carmen during the century of Lepanto and Lope. What Hispanophile would not have advised Maurice Barres or Dr. Rodrigues Larreta to make such a choice! Menard, as if it were the most natural thing in the world, eludes them."

    @Test
    fun `test sentence parsing`() {
        val sentences =  tokenizeSentencesFromDoc(simpleText)
        // println(sentences.contentToString())
        assertEquals(2, sentences.size)
        assertEquals(4, tokenizeSentencesFromDoc(harderText).size)
    }

    @Test
    fun `punctuation is removed correctly`() {
        var punctRemoved = "(A,B)#C:::{A_B}[D]"
        punctRemoved = removePunctuation(punctRemoved)
        assertEquals("ABCABD", punctRemoved)
        println(punctRemoved) // ABCABD
    }

    @Test
    fun `words parsed correctly from sentence`() {
        assertEquals(3, tokenizeWordsFromSentence("This, is text!").size)
    }

    @Test
    fun `test lowercasing`() {
        var firstCapital = "Abc"
        var middleCapital = "aBc"
        assertEquals("abc", firstCapital.lowercase())
        assertEquals("abc",middleCapital.lowercase())
    }

    @Test
    fun `get unique tokens from sentences`() {
//        var sentence = "A a b. B c b d c c" // test for capitalization
        var sentence = "a A a b. B c b d c c"
        var tokenizedSentence = tokenizeWordsFromSentence(sentence)
        var uniqueTokens = uniqueTokensFromArray(tokenizedSentence)
        println(uniqueTokens)
        assertEquals(listOf("a","b","c", "d"), uniqueTokens)
    }

//    @Test
//    fun `get unique targets, without stop words, from sentence`() {
//        var testSetOfTokens = listOf("the","of","dog")
//        var uniqueTokens = uniqueTargetsFromArray(testSetOfTokens)
//        assertEquals(listOf("dog"), uniqueTokens)
//        println(uniqueTokens)
//    }

    @Test
    fun `creates co-occurrence matrix`() {
        var tokenizedSentence = tokenizeWordsFromSentence(simpleText)
        var tokens = uniqueTokensFromArray(tokenizedSentence)
        var cooccurrenceMatrix = generateCooccurrenceMatrix(simpleText, 2)

        var matrixSize = tokens.size
        assertEquals(matrixSize, cooccurrenceMatrix.nrows())
    }

    @Test
    fun `retrieve embedding from co-occurrence matrix`() {
        var tokenizedSentence = tokenizeWordsFromSentence(simpleText)
        var tokens = uniqueTokensFromArray(tokenizedSentence)
        var cooccurrenceMatrix = generateCooccurrenceMatrix(simpleText, 2)
        var targetWordA = "simple"
        wordEmbeddingQuery(targetWordA,tokens,cooccurrenceMatrix)
        var targetWordB = "this"
        wordEmbeddingQuery(targetWordB,tokens,cooccurrenceMatrix)
    }

    @Test
    fun `computes cosine similarity between two vectors`() {
        var tokenizedSentence = tokenizeWordsFromSentence(similarText)
        var tokens = uniqueTokensFromArray(tokenizedSentence)
        var cooccurrenceMatrix = generateCooccurrenceMatrix(similarText, 2)
        var targetWordA = "cat"
        var vectorA = wordEmbeddingQuery(targetWordA,tokens,cooccurrenceMatrix)
        var targetWordB = "dog"
        var vectorB = wordEmbeddingQuery(targetWordB,tokens,cooccurrenceMatrix)
        var targetWordC = "table"
        var vectorC = wordEmbeddingQuery(targetWordC,tokens,cooccurrenceMatrix)

        println(embeddingCosineSimilarity(vectorA, vectorB))
        println(embeddingCosineSimilarity(vectorA, vectorC))
    }

}

