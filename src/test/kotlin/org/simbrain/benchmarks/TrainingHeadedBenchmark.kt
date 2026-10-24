/**
 * Times training of the machine learning demos with the GUI open, per etc/benchmarks/HEADED_SPEC.md. Each demo trains
 * with its own settings and trainer, as a user runs it, so the numbers are what a user sees rather than the tuned
 * headless benchmarks; the headless mode is the same demo without the desktop, the baseline for the GUI's cost.
 *
 * - mlp: the Feed-Forward MNIST demo (batches of 35, a test pass every 10 iterations)
 * - cnn: the CNN MNIST demo (batches of 32, a test pass every 10 iterations)
 * - tinylm: the Tiny Language Model demo trained on the headless benchmark's text, at the size given as
 *   contextSize,embeddingDimension,numHeads,hiddenSize,numLayers (default 24,20,4,30,1, the demo's default size). One
 *   iteration is one pass over the training text, with a test pass every 10.
 *
 * Modes: headless (no desktop), network (desktop with the network window open), dialog (the network window plus the
 * demo's training dialog, which plots error as training runs; training is started through the dialog's own trainer).
 *
 * Usage: TrainingHeadedBenchmarkKt <mlp|cnn|tinylm> <headless|network|dialog> [iterations] [warmupIterations]
 *   [tinyLmSize]
 *
 * Iterations default to 2000 (warm-up 200) for the MNIST demos and 100 (warm-up 10) for the tiny language model. The
 * tiny language model reads its text from simulations/texts, so it runs from the repository root.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.simulations.backprop.cnnMNIST
import org.simbrain.custom_sims.simulations.nlp.tinyLanguageModelSim
import org.simbrain.custom_sims.simulations.tinyMNIST
import org.simbrain.network.NetworkComponent
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.gui.dialogs.CnnTrainerControls
import org.simbrain.network.gui.dialogs.TrainerControls
import org.simbrain.network.gui.dialogs.getCnnTrainingDialog
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.gui.nodes.createTrainingDialog
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.workspace.gui.SimbrainDesktop
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val demo = args[0]
    val mode = args[1]
    require(demo in listOf("mlp", "cnn", "tinylm")) { "demo: mlp | cnn | tinylm" }
    require(mode in listOf("headless", "network", "dialog")) { "mode: headless | network | dialog" }
    val (defaultIterations, defaultWarmup) = if (demo == "tinylm") 100 to 10 else 2000 to 200
    val iterations = args.getOrNull(2)?.toInt() ?: defaultIterations
    val warmup = args.getOrNull(3)?.toInt() ?: defaultWarmup
    val tinyLmSize = args.getOrNull(4) ?: "24,20,4,30,1"
    runBlocking { run(demo, mode, iterations, warmup, tinyLmSize) }
    exitProcess(0)
}

/** The parts of a demo's trainer the benchmark drives; the three trainers share them without a common interface. */
private class DemoTrainer(
    val events: TrainerEvents,
    val stoppingCondition: SupervisedTrainer.StoppingCondition,
    val iteration: () -> Int,
    val start: suspend () -> Unit
) {
    suspend fun trainFor(iterations: Int) {
        val done = CompletableDeferred<Unit>()
        val subscription = events.endTraining.on(Dispatchers.Default) { done.complete(Unit) }
        stoppingCondition.maxIterations = iteration() + iterations
        start()
        done.await()
        subscription.cancel()
    }
}

/**
 * A demo ready to train: its [name] in the output, its [batch] size, how many iterations make one pass over the
 * training data, and what to report once training is done.
 */
private class Demo(
    val name: String,
    val trainer: DemoTrainer,
    val batch: Int,
    val iterationsPerEpoch: Double,
    val result: suspend () -> String
)

// As the demos open them: modal and centered
private fun showDialog(dialog: StandardDialog) = dialog.display()

private suspend fun mlp(component: NetworkComponent, dialog: Boolean): Demo {
    val network = component.network
    val bp = network.allModels.filterIsInstance<BackpropNetwork>().single()
    val trainer = if (dialog) {
        val panel = component.networkPanel()
        withContext(Dispatchers.Swing) {
            val trainingDialog = with(panel) { bp.getSupervisedTrainingDialog() }
            showDialog(trainingDialog)
            val controls = trainingDialog.contentPane.findComponent(TrainerControls::class.java)
                ?: error("no trainer controls in the dialog")
            controls.privateField(TrainerControls::class.java, "trainer") as SupervisedTrainer
        }
    } else {
        SupervisedTrainer(network, bp)
    }
    val batch = (bp.trainerConfig.updateType as? SupervisedTrainer.UpdateMethod.Batch)?.batchSize ?: 1
    return Demo(
        "tiny-mnist-demo",
        DemoTrainer(trainer.events, trainer.config.stoppingCondition, { trainer.iteration }) { trainer.startTraining() },
        batch,
        bp.trainingSet.size / batch.toDouble()
    ) { "\"test_acc\":%.4f".format(trainer.computeTestAccuracy()) }
}

private suspend fun cnn(component: NetworkComponent, dialog: Boolean): Demo {
    val network = component.network
    val cnn = network.getModels<Subnetwork>().filterIsInstance<ConvolutionalNeuralNetwork>().single()
    val trainer = if (dialog) {
        val panel = component.networkPanel()
        withContext(Dispatchers.Swing) {
            val trainingDialog = with(panel) { cnn.getCnnTrainingDialog() }
            showDialog(trainingDialog)
            val controls = trainingDialog.contentPane.findComponent(CnnTrainerControls::class.java)
                ?: error("no trainer controls in the dialog")
            controls.privateField(CnnTrainerControls::class.java, "trainer") as CnnTrainer
        }
    } else {
        // What the training dialog builds
        CnnTrainer(network, cnn.inputTensorLayer, cnn.outputArray, cnn.trainerConfig).apply {
            trainingData = cnn.trainingSet
            testingData = cnn.testingSet
        }
    }
    val batch = trainer.config.batchSize
    return Demo(
        "cnn-mnist-demo",
        DemoTrainer(trainer.events, trainer.config.stoppingCondition, { trainer.iteration }) { trainer.startTraining() },
        batch,
        cnn.trainingSet.size / batch.toDouble()
    ) {
        val test = cnn.testingSet
        var correct = 0
        for (i in test.inputs.indices) {
            val output = trainer.forwardPass(test.inputs[i])
            val target = test.targets[i]
            if (output.indices.maxBy { output[it] } == target.indices.maxBy { target[it] }) correct++
        }
        "\"test_acc\":%.4f".format(correct.toDouble() / test.inputs.size)
    }
}

private suspend fun tinyLm(component: NetworkComponent, dialog: Boolean): Demo {
    val model = component.network.allModels.filterIsInstance<TinyLanguageModel>().single()
    val trainer = model.trainer
    if (dialog) {
        withContext(Dispatchers.Swing) { showDialog(model.createTrainingDialog(SimbrainDesktop.frame)) }
    }
    return Demo(
        "tiny-lm-demo",
        DemoTrainer(trainer.events, trainer.stoppingCondition, { trainer.iteration }) { trainer.startTraining() },
        1,
        1.0
    ) { "\"train_loss\":%.4f,\"windows\":${trainer.trainingWindows.size}".format(trainer.lastTrainingError) }
}

private suspend fun run(demoName: String, mode: String, iterations: Int, warmup: Int, tinyLmSize: String) {
    val headed = mode != "headless"
    val t0 = System.nanoTime()
    val (sim, options) = when (demoName) {
        "mlp" -> tinyMNIST to null
        "cnn" -> cnnMNIST to null
        else -> {
            val (contextSize, embeddingDimension, numHeads, hiddenSize, numLayers) = tinyLmSize.split(",")
            tinyLanguageModelSim to """{"textFile":"casual_texting.txt","contextSize":$contextSize,
                |"embeddingDimension":$embeddingDimension,"numHeads":$numHeads,"hiddenSize":$hiddenSize,
                |"numLayers":$numLayers}""".trimMargin()
        }
    }
    val workspace = openDemo(sim, headed, options)
    val component = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val dialog = mode == "dialog"
    val demo = when (demoName) {
        "mlp" -> mlp(component, dialog)
        "cnn" -> cnn(component, dialog)
        else -> tinyLm(component, dialog)
    }
    val counter = if (headed) component.countFrames() else null
    if (headed) settleDesktop()
    val buildS = (System.nanoTime() - t0) / 1e9

    val t1 = System.nanoTime()
    demo.trainer.trainFor(warmup)
    val warmupS = (System.nanoTime() - t1) / 1e9

    val frames0 = counter?.frames?.get() ?: 0
    val t2 = System.nanoTime()
    demo.trainer.trainFor(iterations)
    val runS = (System.nanoTime() - t2) / 1e9
    val frames = (counter?.frames?.get() ?: 0) - frames0

    val size = if (demoName == "tinylm") ",\"size\":\"$tinyLmSize\"" else ""
    println(
        "{\"simulator\":\"simbrain-gui\",\"benchmark\":\"${demo.name}\"$size,\"mode\":\"$mode\",\"iterations\":$iterations," +
                "\"batch\":${demo.batch},\"build_s\":%.3f,\"warmup_s\":%.3f,\"run_s\":%.4f,\"iterations_per_s\":%.1f,".format(
                    buildS, warmupS, runS, iterations / runS
                ) +
                "\"epoch_equivalent_s\":%.3f,\"frames\":$frames,\"frames_per_s\":%.1f,".format(
                    runS / iterations * demo.iterationsPerEpoch, frames / runS
                ) + demo.result() + "}"
    )
}
