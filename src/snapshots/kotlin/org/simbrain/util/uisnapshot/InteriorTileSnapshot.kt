/**
 * The right-click menus of a tiny language model's interior tiles, laid out in columns: a weight tile, an attention
 * tile, and a residual checkpoint with its plot submenu, so what each kind of tile offers can be checked at a glance.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.TinyLanguageModelNode
import org.simbrain.network.gui.nodes.createTileMenu
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.util.point
import org.simbrain.workspace.gui.SimbrainDesktop
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
