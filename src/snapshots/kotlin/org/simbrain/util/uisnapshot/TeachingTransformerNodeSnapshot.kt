package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.llm.TeachingTransformerConfig
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * Renders the teaching transformer node after a short training run: the residual spine with real
 * values, attention deck with back cards, weight tiles on the diverging palette, op glyphs on the
 * derived edges, and the logit lens docked at the checkpoints.
 */
class TeachingTransformerNodeSnapshot : UiSnapshotDef {
    override val name = "teaching-transformer-node"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(1500, 1600) }

        val teaching = TeachingTransformer(TeachingTransformerConfig(
            contextSize = 12, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 8, numLayers = 1
        )).apply {
            label = "Teaching transformer"
            tokenLabels = arrayListOf("the", "cat", "sat", "on", "a", "mat", ".", "dog")
        }
        val corpus = IntArray(60) { it % 8 }
        teaching.setCorpus(corpus)

        runBlocking { repeat(25) { teaching.trainer.trainOnce() } }

        runBlocking { network.addNetworkModel(teaching, usePlacementManager = false) }
        teaching.location = point(0.0, 0.0)
        teaching.setContext(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 0, 1))
        teaching.forwardContext()

        Thread.sleep(300)
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }
        Thread.sleep(300)
        return panel
    }
}
