/**
 * Headless Tiny MNIST training benchmark used for the Simbrain 4 paper's comparison with PyTorch. Mirrors the
 * tinyMNIST simulation (400-150-100-10, ReLU hidden layers, softmax output, cross-entropy, Adam, batch 35) and times
 * whole epochs of shuffled mini-batches through [org.simbrain.network.trainers.SupervisedTrainer.trainBatch].
 *
 * Usage: MnistBenchmarkKt <dataDir> <epochs> <seed> [hiddenSizes, e.g. 150,100]
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val dataDir = args[0]
    val epochs = args[1].toInt()
    val seed = args[2].toLong()
    val hidden = (args.getOrNull(3) ?: "150,100").split(",").map { it.toInt() }

    val data = TinyMnistData(dataDir)
    val mlp = MnistMlp(data, hidden)
    val rng = Random(seed)
    val epochTimes = List(epochs) { mlp.trainEpoch(rng) }
    val t1 = System.nanoTime()
    val testAccuracy = runBlocking { mlp.trainer.computeTestAccuracy() }
    val evalS = (System.nanoTime() - t1) / 1e9

    val sizes = listOf(data.trainInputs[0].size) + hidden + listOf(data.trainTargets[0].size)
    emitResult(
        "framework" to "simbrain",
        "arch" to sizes.joinToString("-"),
        "batch" to mlp.batchSize,
        "epochs" to epochs,
        "seed" to seed,
        "epoch_s" to epochTimes.map { fixed(it, 3) },
        "test_eval_s" to fixed(evalS, 3),
        "test_acc" to fixed(testAccuracy, 4),
    )
    exitProcess(0)
}
