package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.compositor.HistoryView
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
open class LanguageModelNodeSnapshot : UiSnapshotDef {
    override val name = "language-model-node"

    /** How the scene treats token history: recorded, ghosted to the live row, or not kept. */
    protected open val historyView = HistoryView.FULL

    /** The live-view variant shrinks the window and fills it, so the ghosting reads at fit zoom. */
    protected open val maxSeqLen = 256
    protected open val tokensToGenerate = 24
    protected open val stopAtEndOfText = true

    override fun build(): Component {
        val weightsDir = Lfm2Weights.findWeightsDirectory()
            ?: error("LFM2.5-230M weights not found; run lfm2_export_reference.py or download them once")

        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1300, 1500)
        }

        val languageModel = LanguageModel(weightsDir.toString(), maxSeqLen = maxSeqLen)
        languageModel.initialText = "The capital of France is"
        languageModel.tokensToGenerate = tokensToGenerate
        languageModel.stopAtEndOfText = stopAtEndOfText
        languageModel.historyView = historyView
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

/**
 * Live view over a well-filled window: history rows ghost to faint strength, the live row and
 * the true resident state (KV caches, conv window, weights) keep full strength. The window is
 * small and the budget large so most token rows hold data — at fit zoom a 256-row window with a
 * ten-token run collapses data and live row into one pixel band and the ghosting is invisible.
 */
open class LanguageModelNodeLiveViewSnapshot : LanguageModelNodeSnapshot() {
    override val name = "language-model-node-live-view"
    override val historyView = HistoryView.GHOSTED
    override val maxSeqLen = 48
    override val tokensToGenerate = 36
    override val stopAtEndOfText = false
}

/**
 * No-history mode over the same filled window: the anatomy shows only the live row, the depth
 * strip ghosts (it keeps recording as the replay source), and resident state stays bright.
 */
class LanguageModelNodeNoHistorySnapshot : LanguageModelNodeLiveViewSnapshot() {
    override val name = "language-model-node-no-history"
    override val historyView = HistoryView.OFF
}
