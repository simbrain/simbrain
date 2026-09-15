/** Tests for experience sampling in the triangle reading simulation. */
package org.simbrain.custom_sims.simulations.reading

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WordSamplerTest {
    @Test
    fun `frequency weighted sampling favors high frequency words`() {
        val dataset = ReadingDataset.loadDevelopmentDataset()
        val sampler = WordSampler(dataset, seed = 7)
        val sampled = List(10_000) { dataset.words[sampler.nextIndex(SamplingMode.FREQUENCY_WEIGHTED)] }

        assertTrue(sampled.count { it.frequencyBand == "High" } > sampled.count { it.frequencyBand == "Low" } * 10)
    }

    @Test
    fun `uniform sampling does not privilege high frequency words`() {
        val dataset = ReadingDataset.loadDevelopmentDataset()
        val sampler = WordSampler(dataset, seed = 7)
        val sampled = List(10_000) { dataset.words[sampler.nextIndex(SamplingMode.UNIFORM)] }
        val highProportion = sampled.count { it.frequencyBand == "High" }.toDouble() / sampled.size

        assertTrue(highProportion in 0.45..0.55)
    }
}
