/**
 * Times training of the Feed-Forward MNIST demo with the GUI open, per etc/benchmarks/HEADED_SPEC.md. The trainer uses
 * the demo's own settings (batches of 35, a test pass every 10 iterations), so the numbers are what a user sees, not the
 * tuned headless MnistBenchmark.
 *
 * Modes: headless (no desktop), network (desktop with the network window open), dialog (the network window plus the
 * Train Network dialog, which plots error as training runs; training is started through the dialog's own trainer).
 *
 * Usage: MnistHeadedBenchmarkKt <headless|network|dialog> [iterations=2000] [warmupIterations=200]
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.tinyMNIST
import org.simbrain.network.NetworkComponent
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.dialogs.TrainerControls
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.util.UiWork
import org.simbrain.util.display
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Container
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val mode = args[0]
    require(mode in listOf("headless", "network", "dialog")) { "mode: headless | network | dialog" }
    val iterations = args.getOrNull(1)?.toInt() ?: 2000
    val warmup = args.getOrNull(2)?.toInt() ?: 200
    runBlocking { run(mode, iterations, warmup) }
    exitProcess(0)
}

private fun Container.findTrainerControls(): TrainerControls? {
    if (this is TrainerControls) return this
    return components.filterIsInstance<Container>().firstNotNullOfOrNull { it.findTrainerControls() }
}

private suspend fun SupervisedTrainer.trainFor(iterations: Int) {
    val done = CompletableDeferred<Unit>()
    val subscription = events.endTraining.on(Dispatchers.Default) { done.complete(Unit) }
    config.stoppingCondition.maxIterations = iteration + iterations
    startTraining()
    done.await()
    subscription.cancel()
}

private suspend fun run(mode: String, iterations: Int, warmup: Int) {
    val headed = mode != "headless"
    val t0 = System.nanoTime()
    val workspace: Workspace
    if (headed) {
        SimbrainDesktop.main(arrayOf())
        withContext(Dispatchers.Swing) { SimbrainDesktop.frame.setBounds(0, 0, 1600, 1000) }
        tinyMNIST.run(SimbrainDesktop)
        workspace = SimbrainDesktop.workspace
    } else {
        val scope = SimulationScope()
        tinyMNIST.task(scope, null)
        workspace = scope.workspace
    }
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val network = networkComponent.network
    val bp = network.allModels.filterIsInstance<BackpropNetwork>().single()

    val trainer = if (mode == "dialog") {
        val panel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        withContext(Dispatchers.Swing) {
            val dialog = with(panel) { bp.getSupervisedTrainingDialog() }
            dialog.isModal = false
            dialog.setLocation(820, 10)
            dialog.display()
            val controls = dialog.contentPane.findTrainerControls() ?: error("no trainer controls in the dialog")
            TrainerControls::class.java.getDeclaredField("trainer").apply { isAccessible = true }.get(controls) as SupervisedTrainer
        }
    } else {
        SupervisedTrainer(network, bp)
    }
    if (headed) {
        delay(2000)
        // First-run tips would otherwise cover part of the windows
        SimbrainDesktop.onboardingManager.dismissAll()
        UiWork.awaitIdle()
        SwingUtilities.invokeAndWait { }
    }
    val buildS = (System.nanoTime() - t0) / 1e9

    val t1 = System.nanoTime()
    trainer.trainFor(warmup)
    val warmupS = (System.nanoTime() - t1) / 1e9

    val t2 = System.nanoTime()
    trainer.trainFor(iterations)
    val runS = (System.nanoTime() - t2) / 1e9
    val batch = (bp.trainerConfig.updateType as? SupervisedTrainer.UpdateMethod.Batch)?.batchSize ?: 1
    val examplesPerEpoch = bp.trainingSet.size
    val testAccuracy = trainer.computeTestAccuracy()

    println(
        "{\"simulator\":\"simbrain-gui\",\"benchmark\":\"tiny-mnist-demo\",\"mode\":\"$mode\",\"iterations\":$iterations," +
                "\"batch\":$batch,\"build_s\":%.3f,\"warmup_s\":%.3f,\"run_s\":%.4f,\"iterations_per_s\":%.1f,".format(
                    buildS, warmupS, runS, iterations / runS
                ) +
                "\"epoch_equivalent_s\":%.3f,\"test_acc\":%.4f}".format(runS / iterations * examplesPerEpoch / batch, testAccuracy)
    )
}
