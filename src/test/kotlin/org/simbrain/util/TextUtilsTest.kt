package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextUtilsTest {

    val simpleText = "This is a simple sentence. Ellis will do better."

    val harderText = "In spite of these three obstacles, the fragmentary Don Quixote of Menard is more subtle than that of Cervantes. The latter indulges in a rather coarse opposition between tales of knighthood and the meager, provincial relaity of his country; Menard chooses as 'reality' the land of Carmen during the century of Lepanto and Lope. What Hispanophile would not have advised Maurice Barres or Dr. Rodrigues Larreta to make such a choice! Menard, as if it were the most natural thing in the world, eludes them."

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
}

