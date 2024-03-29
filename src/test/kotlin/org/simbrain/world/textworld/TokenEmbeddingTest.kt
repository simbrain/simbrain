package org.simbrain.world.textworld

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix

class TokenEmbeddingTest {

    var tokenEmbedding = TokenEmbedding(listOf("A", "B"), Matrix.eye(2))

    @Test
    fun `test retrieval of vector given token, for upper and lower case`() {
        assertArrayEquals(doubleArrayOf(1.0, 0.0), tokenEmbedding.get("A"))
        assertArrayEquals(doubleArrayOf(0.0, 1.0), tokenEmbedding.get("B"))
        // Should work for upper or lower case
        assertArrayEquals(doubleArrayOf(1.0, 0.0), tokenEmbedding.get("a"))
        assertArrayEquals(doubleArrayOf(0.0, 1.0), tokenEmbedding.get("b"))
    }

    @Test
    fun `test retrieval of token given vector `() {
        assertEquals("a", tokenEmbedding.getClosestWord(doubleArrayOf(1.0, 0.0)))
        assertEquals("b", tokenEmbedding.getClosestWord(doubleArrayOf(0.0, 1.0)))
    }

    @Test
    fun `return a zero vector when the token is not in map`() {
        assertArrayEquals(DoubleArray(tokenEmbedding.dimension) {0.0}, tokenEmbedding.get("C"))
    }

    @Test
    fun `test metrics on a non-square embedding`() {
        val embedding = TokenEmbedding(listOf("A", "B", "C"),  Matrix(3,2))
        assertEquals(3, embedding.size)
        assertEquals(2, embedding.dimension)
    }

    @Test
    fun `token list and matrix must be same size`() {
        assertThrows<IllegalArgumentException> { TokenEmbedding(listOf("A", "B", "C"),  Matrix(2,3)) }
    }



}