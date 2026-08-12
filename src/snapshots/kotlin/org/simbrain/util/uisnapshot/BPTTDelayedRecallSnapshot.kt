package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.custom_sims.simulations.demos.DelayedRecall
import org.simbrain.custom_sims.simulations.demos.buildDelayedRecallSequence
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities
import kotlin.random.Random

/**
 * The delayed recall simulation's network, to check that its canvas is readable at the size the simulation
 * gives it.
 *
 * This is the widest unrolled view any simulation asks for: the truncation depth is a whole trial, so there
 * are four preceding columns beside the live network, and every layer is in circle mode with labels. The
 * things to look for are columns colliding with each other or with the subnetwork outline, and labels
 * overlapping the recurrent arrow.
 */
class BPTTDelayedRecallSnapshot : UiSnapshotDef {
    override val name = "bptt_delayed_recall"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1500, 800)
        }
        val bptt = BPTTNetwork(DelayedRecall.ALPHABET_SIZE + 1, DelayedRecall.HIDDEN_UNITS, DelayedRecall.ALPHABET_SIZE, point(0, 0))
        runBlocking {
            network.addNetworkModel(bptt, usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            bptt.inputLayer.labelArray = arrayOf("A", "B", "C", "recall")
            bptt.outputLayer.labelArray = arrayOf("A", "B", "C")
            bptt.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }
            bptt.trainerConfig.truncationDepth = DelayedRecall.TRIAL_LENGTH
            bptt.updateStateInfoText()
            bptt.unrolledView = true
        }
        repeat(5) { SwingUtilities.invokeAndWait { } }

        // Trained so the columns hold a real trial rather than zeros, which is what the layout has to fit.
        runBlocking {
            bptt.trainingSet = buildDelayedRecallSequence(DelayedRecall.TRAINING_TRIALS, Random(1))
            val trainer = BPTTTrainer(network, bptt)
            repeat(20) { trainer.trainOnce() }
        }
        Thread.sleep(300)
        repeat(8) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            val content = panel.canvas.layer.fullBounds
            panel.canvas.camera.setViewBounds(
                PBounds(content.x - PAD, content.y - PAD, content.width + 2 * PAD, content.height + 2 * PAD)
            )
        }
        repeat(3) { SwingUtilities.invokeAndWait { } }
        return panel
    }

    companion object {
        private const val PAD = 20.0
    }
}
