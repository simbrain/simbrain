package org.simbrain.network.trainers

import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.matrix


fun testBPProbe() {
    // 2-2-1 xor
    val network = Network()
    val bp = BackpropNetwork(intArrayOf(2, 2, 1), null)
        .also { network.addNetworkModels(it) }
    bp.trainingSet = MatrixDataset(
        inputs = matrix[4, 2](
            0, 0,
            1, 0,
            0, 1,
            1, 1
        ),
        targets = matrix[4, 1](0, 1, 1, 0)
    )
    val trainer = SupervisedTrainer(network, bp).apply {
        config.optimizer = MomentumOptimizer()
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