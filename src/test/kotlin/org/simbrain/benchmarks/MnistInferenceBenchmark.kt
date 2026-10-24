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
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.backprop.cnnMNIST
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.csvToDouble2DArray
import org.simbrain.util.toColumnVector
import java.io.File
import kotlin.random.Random
import kotlin.system.exitProcess

private fun load(dir: String, name: String) = csvToDouble2DArray(File(dir, name).readText())

private fun DoubleArray.argmax() = indices.maxBy { this[it] }

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
    val rng = Random(args[4].toLong())
    require(flavor in listOf("mlp", "cnn")) { "flavor: mlp | cnn" }

    val modes = runBlocking { if (flavor == "mlp") mlp(dataDir, trainEpochs, rng) else cnn(trainEpochs, rng) }
    for ((mode, inference) in modes) {
        inference.setup()
        val results = runBlocking {
            repeat(WARMUP_PASSES) { inference.pass() }
            List(passes) { inference.pass() }
        }
        val n = inference.inputs.size
        val median = results.map { it.first }.sorted()[passes / 2]
        println(
            "{\"framework\":\"simbrain\",\"flavor\":\"$flavor\",\"mode\":\"$mode\",\"batch\":1,\"examples\":$n," +
                    "\"passes\":$passes,\"pass_s\":[${results.joinToString(",") { "%.4f".format(it.first) }}]," +
                    "\"us_per_example\":%.2f,\"examples_per_s\":%.0f,\"test_acc\":%.4f}".format(
                        median / n * 1e6, n / median, results.last().second.toDouble() / n
                    )
        )
    }
    exitProcess(0)
}

private const val WARMUP_PASSES = 3

private suspend fun mlp(dataDir: String, trainEpochs: Int, rng: Random): List<Pair<String, Inference>> {
    val trainX = load(dataDir, "tiny_mnist_train_inputs.csv")
    val trainY = load(dataDir, "tiny_mnist_train_labels.csv")
    val testX = load(dataDir, "tiny_mnist_test_inputs.csv").toList()
    val testY = load(dataDir, "tiny_mnist_test_labels.csv").map { it.argmax() }

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(trainX[0].size, 150, 100, trainY[0].size))
    bp.hiddenLayers().forEach { it.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu } }
    bp.outputLayer.updateRule = SoftmaxRule()
    bp.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
    bp.trainerConfig.learningRate = .001
    bp.trainerConfig.computeAccuracy = false
    bp.initBiases()
    bp.initWeights()
    net.addNetworkModelsAsync(bp, usePlacementManager = false)
    val trainer = SupervisedTrainer(net, bp)
    repeat(trainEpochs) {
        val order = trainX.indices.shuffled(rng)
        bp.trainingSet = TrainingDataset(
            inputs = order.map { trainX[it] }.toMutableList(),
            targets = order.map { trainY[it] }.toMutableList(),
        )
        for (start in order.indices step 35) {
            trainer.iteration++
            trainer.trainBatch(start until minOf(start + 35, order.size))
        }
    }

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
    val scope = SimulationScope()
    cnnMNIST.task.invoke(scope, null)
    val network = (scope.workspace.getComponent("CNN MNIST") as NetworkComponent).network
    val cnn = network.getModels<Subnetwork>().filterIsInstance<ConvolutionalNeuralNetwork>().first()
    val train = cnn.trainingSet
    val test = cnn.testingSet
    val trainer = CnnTrainer(network, cnn.inputTensorLayer, cnn.outputArray, cnn.trainerConfig)
    trainer.config.computeAccuracy = false
    trainer.initParameters()
    repeat(trainEpochs) {
        val order = train.inputs.indices.shuffled(rng)
        trainer.trainingData = TrainingDataset(
            inputs = order.map { train.inputs[it] }.toMutableList(),
            targets = order.map { train.targets[it] }.toMutableList(),
            inputSize = 400,
            targetSize = 10,
        )
        for (start in order.indices step trainer.config.batchSize) {
            trainer.trainBatch(start until minOf(start + trainer.config.batchSize, order.size))
        }
    }

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
