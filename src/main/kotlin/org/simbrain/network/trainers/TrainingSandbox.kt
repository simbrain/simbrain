package org.simbrain.network.trainers

import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork


fun testBPProbe() {
    // 2-2-1 xor
    val network = Network()
    val bp = BackpropNetwork(intArrayOf(2, 2, 1), null)
        .also { network.addNetworkModelsAsync(it) }
    bp.trainingSet = TrainingDataset(
        inputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        ),
        targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )
    )
    val trainer = SupervisedTrainer(network, bp).apply {
        config.optimizer = BasicOptimizer()
    }

    // Probe
    val probe = StructuredProbe.MapProbe()
    trainer.trainBatch(0..3, probe)
    // Shows forward pass only while accumulating deltas, then shows accumulated updates
    print(probe.toTreeString())
}

fun main() {
    testBPProbe()
}
