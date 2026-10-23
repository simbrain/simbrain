/**
 * Snapshots of the interior tile actions on a tiny language model: the right-click menus for a
 * weight tile, an attention tile, and a residual checkpoint with its plot submenu, and the tile dialog with its properties and weights table,
 * including on the LFM's largest weight matrix.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.LanguageModelNode
import org.simbrain.network.gui.nodes.TinyLanguageModelNode
import org.simbrain.network.gui.nodes.createTileDialog
import org.simbrain.network.gui.nodes.createTileMenu
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.util.point
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

private fun tinyModelPanel(): Pair<NetworkPanel, TinyLanguageModelNode> {
    val component = NetworkComponent("snapshot")
    SimbrainDesktop.workspace.addWorkspaceComponent(component)
    val panel = NetworkPanel(component).apply { preferredSize = Dimension(1200, 1200) }
    val model = TinyLanguageModel(TinyLmConfig(
        contextSize = 12, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 8, numLayers = 1
    )).apply {
        label = "Tiny language model"
        tokenLabels = arrayListOf("the", "cat", "sat", "on", "a", "mat", ".", "dog")
    }
    runBlocking { component.network.addNetworkModel(model, usePlacementManager = false) }
    model.location = point(0.0, 0.0)
    model.setContext(intArrayOf(0, 1, 2, 3))
    model.forwardContext()
    Thread.sleep(300)
    lateinit var node: TinyLanguageModelNode
    SwingUtilities.invokeAndWait {
        JDialog().apply { contentPane = panel; pack() }
        node = panel.filterScreenElements<TinyLanguageModelNode>().single()
    }
    return panel to node
}

/** The popup only paints once realized, so its real items render in a plain column instead. */
private fun menuColumn(title: String, menu: JPopupMenu) = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
    add(JLabel(title).apply { border = BorderFactory.createEmptyBorder(0, 4, 6, 4) })
    menu.components.forEach { add(it) }
}

class InteriorTileMenuSnapshot : UiSnapshotDef {
    override val name = "interior-tile-menus"

    override fun build(): Component {
        val (panel, node) = tinyModelPanel()
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val scene = node.tinyLanguageModel.scene
            val weightMenu = panel.createTileMenu(node, scene.tile("layers.0.attn.wq"))
            val attentionMenu = panel.createTileMenu(node, scene.tile("layers.0.attn.weights"))
            val checkpointMenu = panel.createTileMenu(node, scene.tile("layers.0.resid"))
            val plotMenu = checkpointMenu.components.filterIsInstance<JMenu>().first()
            host = JFrame().apply {
                contentPane = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(menuColumn("Weight tile (Wq)", weightMenu))
                    add(menuColumn("Attention tile", attentionMenu))
                    add(menuColumn("Checkpoint tile", checkpointMenu))
                    add(menuColumn(plotMenu.text, plotMenu.popupMenu))
                }
                pack()
            }
        }
        return host
    }
}

class InteriorTileDialogSnapshot : UiSnapshotDef {
    override val name = "interior-tile-dialog"

    override fun build(): Component {
        val (panel, node) = tinyModelPanel()
        return panel.createTileDialog(node, node.tinyLanguageModel.scene.tile("layers.0.attn.wq"))
    }
}

class InteriorTileActivationDialogSnapshot : UiSnapshotDef {
    override val name = "interior-tile-activation-dialog"

    override fun build(): Component {
        val (panel, node) = tinyModelPanel()
        return panel.createTileDialog(node, node.tinyLanguageModel.scene.tile("resid0"))
    }
}

class InteriorTileWeightsTabSnapshot : UiSnapshotDef {
    override val name = "interior-tile-weights-tab"

    override fun build(): Component {
        val (panel, node) = tinyModelPanel()
        return panel.createTileDialog(node, node.tinyLanguageModel.scene.tile("layers.0.attn.wq")).apply {
            (contentPane as JTabbedPane).selectedIndex = 1
        }
    }
}

/**
 * The dialog on the LFM's largest weight matrix (conv in_proj, 3072 x 1024) with its build time
 * printed, as the check that the weights table stays usable at real scale. Needs the LFM2 weights.
 */
class InteriorTileLfmDialogSnapshot : UiSnapshotDef {
    override val name = "interior-tile-lfm-dialog"

    override fun build(): Component {
        val weightsDir = Lfm2Weights.findWeightsDirectory()
            ?: error("LFM2.5-230M weights not found in the Simbrain or HF cache")
        val component = NetworkComponent("snapshot")
        SimbrainDesktop.workspace.addWorkspaceComponent(component)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(1200, 1200) }
        val model = LanguageModel(weightsDir.toString(), maxSeqLen = 64)
        runBlocking { component.network.addNetworkModel(model, usePlacementManager = false) }
        model.loadWeights()
        model.selectedLayer = 3
        model.loaded!!.scene.layerSelector?.invoke(3)
        Thread.sleep(300)
        lateinit var node: LanguageModelNode
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            node = panel.filterScreenElements<LanguageModelNode>().single()
        }
        val start = System.nanoTime()
        val dialog = panel.createTileDialog(node, model.loaded!!.scene.tile("block.w.conv.in_proj.weight"))
        println("Tile dialog for in_proj built in ${(System.nanoTime() - start) / 1_000_000} ms")
        return dialog.apply { (contentPane as JTabbedPane).selectedIndex = 1 }
    }
}
