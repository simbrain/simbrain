/**
 * Headless Tiny MNIST training benchmark used for the Simbrain 4 paper's comparison with PyTorch. Mirrors the
 * tinyMNIST simulation (400-150-100-10, ReLU hidden layers, softmax output, cross-entropy, Adam, batch 35) and times
 * whole epochs of shuffled mini-batches through [SupervisedTrainer.trainBatch].
 *
 * Usage: MnistBenchmarkKt <dataDir> <epochs> <seed> [hiddenSizes, e.g. 150,100]
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.csvToDouble2DArray
import java.io.File
import kotlin.random.Random
import kotlin.system.exitProcess

private fun load(dir: String, name: String) = csvToDouble2DArray(File(dir, name).readText())

fun main(args: Array<String>) {
    val dataDir = args[0]
    val epochs = args[1].toInt()
    val rng = Random(args[2].toLong())
    val hidden = (args.getOrNull(3) ?: "150,100").split(",").map { it.toInt() }
    val batchSize = 35

    val trainX = load(dataDir, "tiny_mnist_train_inputs.csv")
    val trainY = load(dataDir, "tiny_mnist_train_labels.csv")
    val testX = load(dataDir, "tiny_mnist_test_inputs.csv")
    val testY = load(dataDir, "tiny_mnist_test_labels.csv")

    val net = Network()
    val bp = BackpropNetwork((listOf(trainX[0].size) + hidden + listOf(trainY[0].size)).toIntArray())
    bp.hiddenLayers().forEach { it.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu } }
    bp.outputLayer.updateRule = SoftmaxRule()
    bp.testingSet = TrainingDataset(
        inputs = testX.map { it.toMutableList() }.toMutableList(),
        targets = testY.map { it.toMutableList() }.toMutableList(),
    )
    bp.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainerConfig.learningRate = .001
    bp.trainerConfig.computeAccuracy = false
    bp.initBiases()
    bp.initWeights()
    net.addNetworkModelsAsync(bp, usePlacementManager = false)
    val trainer = SupervisedTrainer(net, bp)

    val epochTimes = mutableListOf<Double>()
    for (epoch in 0 until epochs) {
        val order = trainX.indices.shuffled(rng)
        bp.trainingSet = TrainingDataset(
            inputs = order.map { trainX[it].toMutableList() }.toMutableList(),
            targets = order.map { trainY[it].toMutableList() }.toMutableList(),
        )
        val t0 = System.nanoTime()
        var start = 0
        while (start < order.size) {
            val end = minOf(start + batchSize, order.size)
            // One trainer iteration per batch, as when training from the GUI (Adam's bias correction counts these)
            trainer.iteration++
            trainer.trainBatch(start until end)
            start = end
        }
        epochTimes += (System.nanoTime() - t0) / 1e9
    }
    val t1 = System.nanoTime()
    val testAccuracy = runBlocking { trainer.computeTestAccuracy() }
    val evalS = (System.nanoTime() - t1) / 1e9

    println(
        "{\"framework\":\"simbrain\",\"arch\":\"${(listOf(trainX[0].size) + hidden + listOf(10)).joinToString("-")}\"," +
                "\"batch\":$batchSize,\"epochs\":$epochs,\"epoch_s\":[${epochTimes.joinToString(",") { "%.3f".format(it) }}]," +
                "\"test_eval_s\":%.3f,\"test_acc\":%.4f}".format(evalS, testAccuracy)
    )
    exitProcess(0)
}
