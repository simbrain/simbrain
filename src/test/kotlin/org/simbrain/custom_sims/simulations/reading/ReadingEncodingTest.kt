/** Tests for the stable, inspectable representations used by the triangle reading simulation. */
package org.simbrain.custom_sims.simulations.reading

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReadingEncodingTest {
    @Test
    fun `orthographic encoding has fixed deterministic dimensionality`() {
        val encoder = OrthographyEncoder()

        assertEquals(135, encoder.dimension)
        assertArrayEquals(encoder.encode("mint"), encoder.encode("mint"))
        assertEquals(5, encoder.encode("mint").count { it == 1.0 })
    }

    @Test
    fun `phonological development words round trip through feature slots`() {
        val dataset = ReadingDataset.loadDevelopmentDataset()
        val encoder = PhonologyEncoder()

        dataset.words.forEach { word ->
            assertEquals(word.pronunciation, encoder.decode(encoder.encode(word.pronunciation)), word.word)
        }
    }

    @Test
    fun `development dataset has balanced frequency and regularity conditions`() {
        val dataset = ReadingDataset.loadDevelopmentDataset()

        assertEquals(32, dataset.words.size)
        assertEquals(8, dataset.words.count { it.frequencyBand == "High" && it.regularity == Regularity.REGULAR })
        assertEquals(8, dataset.words.count { it.frequencyBand == "High" && it.regularity == Regularity.EXCEPTION })
        assertEquals(8, dataset.words.count { it.frequencyBand == "Low" && it.regularity == Regularity.REGULAR })
        assertEquals(8, dataset.words.count { it.frequencyBand == "Low" && it.regularity == Regularity.EXCEPTION })
    }
}
