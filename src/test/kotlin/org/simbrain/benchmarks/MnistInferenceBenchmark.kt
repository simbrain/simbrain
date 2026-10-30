/**
 * Headless inference benchmark for the two MNIST demos, for the Simbrain 4 paper's PyTorch comparison
 * (mnist_infer_torch.py). Trains briefly so the accuracy check means something, then times passes over the test set,
 * one example at a time, the only way Simbrain runs these networks:
 *
 * - forward: the trainer's forward pass alone (BackpropNetwork.forwardPass, CnnTrainer.forwardPass)
 * - update: the input layer clamped to the example and one network update, as when the simulation is run
 *
 * The MLP is MnistBenchmark's 400-150-100-10 network; the CNN is the cnnMNIST simulation itself.
 *
 * Usage: MnistInferenceBenchmarkKt <mlp|cnn> <dataDir> <trainEpochs> <passes> <seed>
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import org.simbrain.util.toColumnVector
import kotlin.random.Random
import kotlin.system.exitProcess

/** One timed pass: runs [infer] on every test example and returns the seconds taken and the number correct. */
private class Inference(
    val inputs: List<DoubleArray>,
    val labels: List<Int>,
    val setup: () -> Unit = {},
    val infer: suspend (DoubleArray) -> Int
) {
    suspend fun pass(): Pair<Double, Int> {
        var correct = 0
        val t0 = System.nanoTime()
        for (i in inputs.indices) {
            if (infer(inputs[i]) == labels[i]) correct++
        }
        return (System.nanoTime() - t0) / 1e9 to correct
    }
}

fun main(args: Array<String>) {
    val flavor = args[0]
    val dataDir = args[1]
    val trainEpochs = args[2].toInt()
    val passes = args[3].toInt()
    val seed = args[4].toLong()
    require(flavor in listOf("mlp", "cnn")) { "flavor: mlp | cnn" }

    val rng = Random(seed)
    val modes = runBlocking { if (flavor == "mlp") mlp(dataDir, trainEpochs, rng) else cnn(trainEpochs, rng) }
    for ((mode, inference) in modes) {
        inference.setup()
        val results = runBlocking {
            repeat(WARMUP_PASSES) { inference.pass() }
            List(passes) { inference.pass() }
        }
        val n = inference.inputs.size
        val median = results.map { it.first }.sorted()[passes / 2]
        emitResult(
            "framework" to "simbrain",
            "flavor" to flavor,
            "mode" to mode,
            "batch" to 1,
            "examples" to n,
            "passes" to passes,
            "seed" to seed,
            "pass_s" to results.map { fixed(it.first, 4) },
            "us_per_example" to fixed(median / n * 1e6, 2),
            "examples_per_s" to fixed(n / median, 0),
            "test_acc" to fixed(results.last().second.toDouble() / n, 4),
        )
    }
    exitProcess(0)
}

private const val WARMUP_PASSES = 3

private fun mlp(dataDir: String, trainEpochs: Int, rng: Random): List<Pair<String, Inference>> {
    val data = TinyMnistData(dataDir)
    val model = MnistMlp(data)
    repeat(trainEpochs) { model.trainEpoch(rng) }
    val bp = model.bp
    val net = model.network

    val testX = data.testInputs.toList()
    val testY = data.testTargets.map { it.argmax() }
    val forward = Inference(testX, testY) { x ->
        bp.inputLayer.activations = x.toColumnVector()
        with(net) { bp.forwardPass() }
        bp.outputLayer.activations.col(0).argmax()
    }
    val update = Inference(testX, testY, setup = { bp.inputLayer.isClamped = true }) { x ->
        bp.inputLayer.setActivations(x)
        net.updateSuspend()
        bp.outputLayer.activations.col(0).argmax()
    }
    return listOf("forward" to forward, "update" to update)
}

private suspend fun cnn(trainEpochs: Int, rng: Random): List<Pair<String, Inference>> {
    val model = MnistCnn.create()
    repeat(trainEpochs) { model.trainEpoch(rng) }
    val cnn = model.cnn
    val network = model.network
    val trainer = model.trainer

    val test = cnn.testingSet
    val labels = test.targets.map { it.argmax() }
    val forward = Inference(test.inputs, labels) { x -> trainer.forwardPass(x).argmax() }
    val input = cnn.inputTensorLayer
    val update = Inference(test.inputs, labels, setup = { input.isClamped = true }) { x ->
        System.arraycopy(x, 0, input.activations, 0, x.size)
        network.updateSuspend()
        cnn.outputArray.activations.col(0).argmax()
    }
    return listOf("forward" to forward, "update" to update)
}
