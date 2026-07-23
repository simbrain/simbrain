package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.Lfm2Weights
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * The LFM2 language model as a network-canvas node after a real generation run: interaction box,
 * compositor interior, and the status line with the generated text. Needs the LFM2.5-230M weights
 * (HF cache or Simbrain cache).
 */
class LanguageModelNodeSnapshot : UiSnapshotDef {
    override val name = "language-model-node"

    override fun build(): Component {
        val weightsDir = Lfm2Weights.findWeightsDirectory()
            ?: error("LFM2.5-230M weights not found; run lfm2_export_reference.py or download them once")

        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1300, 1500)
        }

        val languageModel = LanguageModel(weightsDir.toString(), maxSeqLen = 256)
        languageModel.prompt = "The capital of France is"
        languageModel.tokensToGenerate = 24
        languageModel.loadWeights()
        runBlocking { network.addNetworkModel(languageModel, usePlacementManager = false) }

        while (languageModel.canAdvance) {
            languageModel.step()
        }
        Thread.sleep(200)

        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }
        Thread.sleep(200)
        return panel
    }
}
