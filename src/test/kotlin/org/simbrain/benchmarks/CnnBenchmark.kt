/**
 * Headless CNN training benchmark for the Simbrain 4 paper's PyTorch comparison. Builds the cnnMNIST simulation
 * (Tiny MNIST, conv 3x3x5 -> maxpool -> conv 3x3x8 -> maxpool -> dense 10, Adam, batch 32) and times whole epochs of
 * shuffled mini-batches through [CnnTrainer.trainBatch].
 *
 * Usage: CnnBenchmarkKt <epochs> <seed>
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.backprop.cnnMNIST
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.TrainingDataset
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val epochs = args[0].toInt()
    val rng = Random(args[1].toLong())

    val scope = SimulationScope()
    runBlocking { cnnMNIST.task.invoke(scope, null) }
    val network = (scope.workspace.getComponent("CNN MNIST") as NetworkComponent).network
    val cnn = network.getModels<Subnetwork>().filterIsInstance<ConvolutionalNeuralNetwork>().first()
    val train = cnn.trainingSet
    val test = cnn.testingSet
    val trainer = CnnTrainer(network, cnn.inputTensorLayer, cnn.outputArray, cnn.trainerConfig)
    trainer.config.computeAccuracy = false
    trainer.initParameters()
    val batchSize = trainer.config.batchSize

    val epochTimes = mutableListOf<Double>()
    for (epoch in 0 until epochs) {
        val order = train.inputs.indices.shuffled(rng)
        trainer.trainingData = TrainingDataset(
            inputs = order.map { train.inputs[it] }.toMutableList(),
            targets = order.map { train.targets[it] }.toMutableList(),
            inputSize = 400,
            targetSize = 10,
        )
        val t0 = System.nanoTime()
        var start = 0
        while (start < order.size) {
            val end = minOf(start + batchSize, order.size)
            trainer.trainBatch(start until end)
            start = end
        }
        epochTimes += (System.nanoTime() - t0) / 1e9
    }
    var correct = 0
    for (i in test.inputs.indices) {
        val output = trainer.forwardPass(test.inputs[i])
        val target = test.targets[i]
        if (output.indices.maxBy { output[it] } == target.indices.maxBy { target[it] }) correct++
    }
    println(
        "{\"framework\":\"simbrain-cnn\",\"batch\":$batchSize,\"epochs\":$epochs," +
                "\"epoch_s\":[${epochTimes.joinToString(",") { "%.3f".format(it) }}]," +
                "\"test_acc\":%.4f}".format(correct.toDouble() / test.inputs.size)
    )
    exitProcess(0)
}
