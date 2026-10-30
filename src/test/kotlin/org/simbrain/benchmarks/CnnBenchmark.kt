/**
 * Headless CNN training benchmark for the Simbrain 4 paper's PyTorch comparison. Builds the cnnMNIST simulation
 * (Tiny MNIST, conv 3x3x5 -> maxpool -> conv 3x3x8 -> maxpool -> dense 10, Adam, batch 32) and times whole epochs of
 * shuffled mini-batches through [org.simbrain.network.trainers.CnnTrainer.trainBatch].
 *
 * Usage: CnnBenchmarkKt <epochs> <seed>
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val epochs = args[0].toInt()
    val seed = args[1].toLong()

    val model = runBlocking { MnistCnn.create() }
    val rng = Random(seed)
    val epochTimes = List(epochs) { model.trainEpoch(rng) }
    emitResult(
        "framework" to "simbrain-cnn",
        "batch" to model.trainer.config.batchSize,
        "epochs" to epochs,
        "seed" to seed,
        "epoch_s" to epochTimes.map { fixed(it, 3) },
        "test_acc" to fixed(model.trainer.testAccuracy(model.cnn.testingSet), 4),
    )
    exitProcess(0)
}
