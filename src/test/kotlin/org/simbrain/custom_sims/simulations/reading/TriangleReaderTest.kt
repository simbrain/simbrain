/** Integration-level checks for feed-forward reading training and inference. */
package org.simbrain.custom_sims.simulations.reading

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.AdamOptimizer

class TriangleReaderTest {
    @Test
    fun `training lowers error on the small development dataset`() = runBlocking {
        val dataset = ReadingDataset.loadDevelopmentDataset()
        val orthography = OrthographyEncoder()
        val phonology = PhonologyEncoder()
        val network = Network()
        val model = BackpropNetwork(intArrayOf(orthography.dimension, 40, phonology.dimension), null).apply {
            trainerConfig.optimizer = AdamOptimizer()
            trainerConfig.learningRate = 0.01
            initWeights()
            initBiases()
        }
        network.addNetworkModels(model)
        val reader = TriangleReader(network, model, dataset, orthography, phonology)
        val before = reader.evaluateAll().map { it.error }.average()

        reader.trainSampled(2_000, SamplingMode.UNIFORM)
        val after = reader.evaluateAll().map { it.error }.average()

        assertTrue(after < before, "Expected training to lower mean error: before=$before after=$after")
    }
}
