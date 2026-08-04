package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.custom_sims.simulations.demos.Curriculum
import org.simbrain.custom_sims.simulations.demos.buildVariableGapSequence
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
 * The curriculum simulation's network.
 *
 * Eight hidden units in circle mode make this the widest layer any simulation draws unrolled, which is the
 * case that used to be painted over its own recurrent arrow before the column spacing was derived from the
 * layer width. The truncation depth is two, so there is one prior column: the picture should make it
 * obvious how little of the trial the window covers.
 */
class BPTTCurriculumSnapshot : UiSnapshotDef {
    override val name = "bptt_curriculum"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1100, 720)
        }
        val bptt = BPTTNetwork(
            Curriculum.ALPHABET_SIZE + 1, Curriculum.HIDDEN_UNITS, Curriculum.ALPHABET_SIZE, point(0, 0)
        )
        runBlocking {
            network.addNetworkModel(bptt, usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            bptt.inputLayer.labelArray = arrayOf("A", "B", "C", "recall")
            bptt.outputLayer.labelArray = arrayOf("A", "B", "C")
            bptt.layers.filterIsInstance<NeuronArray>().forEach { it.circleMode = true }
            bptt.trainerConfig.truncationDepth = Curriculum.TRUNCATION_DEPTH
            bptt.updateStateInfoText()
            bptt.unrolledView = true
        }
        repeat(5) { SwingUtilities.invokeAndWait { } }

        runBlocking {
            bptt.trainingSet = buildVariableGapSequence(16, Curriculum.GAPS, Random(1))
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
