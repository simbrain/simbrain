/**
 * Times the two MNIST demos classifying their test set with the GUI open, per etc/benchmarks/HEADED_SPEC.md. Each
 * example is one workspace step with the example clamped on the input layer, what the input table's "Apply inputs" does
 * row by row, and the headless MnistInferenceBenchmark's "update" mode with the network window open.
 *
 * Modes: headless (no desktop), paced (each step drawn before the next, as when stepping through examples by hand),
 * fast (runAsFastAsPossible; the view refreshes at frame rate).
 *
 * The demo is first trained for [trainIterations] batches so its accuracy shows the outputs are right.
 *
 * Usage: InferenceHeadedBenchmarkKt <mlp|cnn> <headless|paced|fast> [examples=10000] [warmupExamples=5000]
 *   [trainIterations=300]
 *
 * Examples cycle through the 1000-example test set; a shorter run suits paced mode, which draws every example.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.simulations.backprop.cnnMNIST
import org.simbrain.custom_sims.simulations.tinyMNIST
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Layer
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.util.updateAction
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val demo = args[0]
    val mode = args[1]
    require(demo in listOf("mlp", "cnn")) { "demo: mlp | cnn" }
    require(mode in listOf("headless", "paced", "fast")) { "mode: headless | paced | fast" }
    val examples = args.getOrNull(2)?.toInt() ?: 10000
    val warmup = args.getOrNull(3)?.toInt() ?: 5000
    val trainIterations = args.getOrNull(4)?.toInt() ?: 300
    runBlocking { run(demo, mode, examples, warmup, trainIterations) }
    exitProcess(0)
}

/** A trained demo's test set and how to put an example on its input layer. */
private class Classifier(
    val name: String,
    val inputs: List<DoubleArray>,
    val labels: List<Int>,
    val output: Layer,
    val apply: (DoubleArray) -> Unit
)

private fun DoubleArray.argmax() = indices.maxBy { this[it] }

private suspend fun mlp(component: NetworkComponent, trainIterations: Int): Classifier {
    val network = component.network
    val bp = network.allModels.filterIsInstance<BackpropNetwork>().single()
    val trainer = SupervisedTrainer(network, bp)
    repeat(trainIterations) { trainer.trainOnce() }
    bp.inputLayer.isClamped = true
    val test = bp.testingSet
    return Classifier("tiny-mnist-demo", test.inputs, test.targets.map { it.argmax() }, bp.outputLayer) {
        bp.inputLayer.setActivations(it)
    }
}

private suspend fun cnn(component: NetworkComponent, trainIterations: Int): Classifier {
    val network = component.network
    val cnn = network.getModels<Subnetwork>().filterIsInstance<ConvolutionalNeuralNetwork>().single()
    val trainer = CnnTrainer(network, cnn.inputTensorLayer, cnn.outputArray, cnn.trainerConfig).apply {
        trainingData = cnn.trainingSet
        testingData = cnn.testingSet
    }
    repeat(trainIterations) { trainer.trainOnce() }
    val input = cnn.inputTensorLayer
    input.isClamped = true
    val test = cnn.testingSet
    return Classifier("cnn-mnist-demo", test.inputs, test.targets.map { it.argmax() }, cnn.outputArray) {
        System.arraycopy(it, 0, input.activations, 0, it.size)
    }
}

private suspend fun run(demoName: String, mode: String, examples: Int, warmup: Int, trainIterations: Int) {
    val headed = mode != "headless"
    val t0 = System.nanoTime()
    val workspace = openDemo(if (demoName == "mlp") tinyMNIST else cnnMNIST, headed)
    val component = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val network = component.network
    val classifier = if (demoName == "mlp") mlp(component, trainIterations) else cnn(component, trainIterations)
    workspace.runAsFastAsPossible = mode == "fast"

    // After each network update, score the example just classified and clamp the next one for the following step
    val n = classifier.inputs.size
    var current = 0
    var counting = false
    var correct = 0
    classifier.apply(classifier.inputs[0])
    network.addUpdateAction(updateAction("Next test example") {
        if (counting && classifier.output.activations.col(0).argmax() == classifier.labels[current % n]) correct++
        current++
        classifier.apply(classifier.inputs[current % n])
    })

    val counter = if (headed) component.countFrames() else null
    if (headed) settleDesktop()
    val buildS = (System.nanoTime() - t0) / 1e9

    val t1 = System.nanoTime()
    workspace.iterateSuspend(warmup)
    val warmupS = (System.nanoTime() - t1) / 1e9

    counting = true
    val frames0 = counter?.frames?.get() ?: 0
    val t2 = System.nanoTime()
    workspace.iterateSuspend(examples)
    val runS = (System.nanoTime() - t2) / 1e9
    val frames = (counter?.frames?.get() ?: 0) - frames0
    counting = false

    println(
        "{\"simulator\":\"simbrain-gui\",\"benchmark\":\"${classifier.name}-inference\",\"mode\":\"$mode\"," +
                "\"examples\":$examples,\"build_s\":%.3f,\"warmup_s\":%.3f,\"run_s\":%.4f,\"us_per_example\":%.2f,".format(
                    buildS, warmupS, runS, runS / examples * 1e6
                ) +
                "\"examples_per_s\":%.1f,\"frames\":$frames,\"frames_per_s\":%.1f,\"test_acc\":%.4f}".format(
                    examples / runS, frames / runS, correct.toDouble() / examples
                )
    )
}
