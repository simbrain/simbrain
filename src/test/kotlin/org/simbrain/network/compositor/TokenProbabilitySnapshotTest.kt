package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenProbabilitySnapshotTest {

    @Test
    fun `full snapshot preserves vocabulary order and sampled token`() {
        val snapshot = TokenProbabilitySnapshot.full(doubleArrayOf(0.1, 0.7, 0.2), sampledTokenId = 2)

        assertEquals(listOf(0, 1, 2), snapshot.entries.map { it.tokenId })
        assertEquals(1, snapshot.argmaxTokenId)
        assertEquals(2, snapshot.sampledTokenId)
        assertTrue(snapshot.showAll)
    }

    @Test
    fun `top k keeps the sampled token when it is outside the candidates`() {
        val snapshot = TokenProbabilitySnapshot.topK(
            floatArrayOf(0f, 4f, 1f, 3f, -2f), count = 2, sampledTokenId = 4,
        )

        assertEquals(listOf(1, 3, 4), snapshot.entries.map { it.tokenId })
        assertEquals(1, snapshot.argmaxTokenId)
        assertEquals(4, snapshot.sampledTokenId)
        assertTrue(snapshot.entries[0].probability > snapshot.entries[1].probability)
    }

    @Test
    fun `top k temperature changes normalized candidate probabilities`() {
        val cold = TokenProbabilitySnapshot.topK(floatArrayOf(0f, 1f), 2, 1, temperature = 0.5)
        val warm = TokenProbabilitySnapshot.topK(floatArrayOf(0f, 1f), 2, 1, temperature = 2.0)

        assertTrue(cold.entries.first { it.tokenId == 1 }.probability > warm.entries.first { it.tokenId == 1 }.probability)
    }
}
