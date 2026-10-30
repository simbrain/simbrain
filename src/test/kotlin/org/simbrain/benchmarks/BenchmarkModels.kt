/**
 * The two Tiny MNIST networks the machine learning harnesses train and time, shared by the training and inference
 * benchmarks: the tinyMNIST demo's multilayer perceptron built from the data files, and the cnnMNIST simulation's
 * CNN. Each trains one shuffled epoch at a time, with only the batches timed.
 */
package org.simbrain.benchmarks

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
import java.io.File
import kotlin.random.Random

internal fun DoubleArray.argmax() = indices.maxBy { this[it] }

/** The Tiny MNIST files in [dataDir], as `bench setup` downloads them. */
internal class TinyMnistData(dataDir: String) {
    val trainInputs = load(dataDir, "tiny_mnist_train_inputs.csv")
    val trainTargets = load(dataDir, "tiny_mnist_train_labels.csv")
    val testInputs = load(dataDir, "tiny_mnist_test_inputs.csv")
    val testTargets = load(dataDir, "tiny_mnist_test_labels.csv")

    private fun load(dir: String, name: String) = csvToDouble2DArray(File(dir, name).readText())
}

/**
 * The tinyMNIST demo's network: ReLU hidden layers of the [hidden] sizes, a softmax output, cross-entropy and Adam at
 * 0.001, trained in batches of 35 through [SupervisedTrainer.trainBatch]. Its testing set is the data's test set.
 */
internal class MnistMlp(private val data: TinyMnistData, hidden: List<Int> = listOf(150, 100)) {
    val network = Network()
    val bp = BackpropNetwork((listOf(data.trainInputs[0].size) + hidden + listOf(data.trainTargets[0].size)).toIntArray())
    val trainer: SupervisedTrainer
    val batchSize = 35

    init {
        bp.hiddenLayers().forEach { it.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu } }
        bp.outputLayer.updateRule = SoftmaxRule()
        bp.testingSet = TrainingDataset(data.testInputs.toMutableList(), data.testTargets.toMutableList())
        bp.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        bp.trainerConfig.learningRate = .001
        bp.trainerConfig.computeAccuracy = false
        bp.initBiases()
        bp.initWeights()
        network.addNetworkModelsAsync(bp, usePlacementManager = false)
        trainer = SupervisedTrainer(network, bp)
    }

    /** One pass over the training data in an order shuffled by [rng]; returns the seconds the batches took. */
    fun trainEpoch(rng: Random): Double {
        val order = data.trainInputs.indices.shuffled(rng)
        bp.trainingSet = TrainingDataset(
            order.map { data.trainInputs[it] }.toMutableList(),
            order.map { data.trainTargets[it] }.toMutableList(),
        )
        val t0 = System.nanoTime()
        for (start in order.indices step batchSize) {
            // One trainer iteration per batch, as when training from the GUI (Adam's bias correction counts these)
            trainer.iteration++
            trainer.trainBatch(start until minOf(start + batchSize, order.size))
        }
        return (System.nanoTime() - t0) / 1e9
    }
}

/** The cnnMNIST simulation's network and a trainer for it, built as its training dialog builds one. */
internal class MnistCnn private constructor(val network: Network, val cnn: ConvolutionalNeuralNetwork) {
    val trainer = CnnTrainer(network, cnn.inputTensorLayer, cnn.outputArray, cnn.trainerConfig).apply {
        config.computeAccuracy = false
        initParameters()
    }

    /** One pass over the training data in an order shuffled by [rng]; returns the seconds the batches took. */
    fun trainEpoch(rng: Random): Double {
        val train = cnn.trainingSet
        val order = train.inputs.indices.shuffled(rng)
        trainer.trainingData = TrainingDataset(
            inputs = order.map { train.inputs[it] }.toMutableList(),
            targets = order.map { train.targets[it] }.toMutableList(),
            inputSize = 400,
            targetSize = 10,
        )
        val batchSize = trainer.config.batchSize
        val t0 = System.nanoTime()
        for (start in order.indices step batchSize) {
            trainer.trainBatch(start until minOf(start + batchSize, order.size))
        }
        return (System.nanoTime() - t0) / 1e9
    }

    companion object {
        suspend fun create(): MnistCnn {
            val scope = SimulationScope()
            cnnMNIST.task.invoke(scope, null)
            val network = (scope.workspace.getComponent("CNN MNIST") as NetworkComponent).network
            return MnistCnn(network, network.getModels<Subnetwork>().filterIsInstance<ConvolutionalNeuralNetwork>().first())
        }
    }
}

/** The share of [test] this trainer's forward pass classifies correctly. */
internal fun CnnTrainer.testAccuracy(test: TrainingDataset): Double {
    var correct = 0
    for (i in test.inputs.indices) {
        if (forwardPass(test.inputs[i]).argmax() == test.targets[i].argmax()) correct++
    }
    return correct.toDouble() / test.inputs.size
}
