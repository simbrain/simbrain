/**
 * Shows the "Edit selected models" menu item label and tooltip for a range of selections, from a single model type
 * through mixed selections and selections containing models without an edit dialog.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.PoolingConnector
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createSelectionEditMenu
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.gui.nodes.TensorNode
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.SwingUtilities

class EditSelectedModelsLabelSnapshot : UiSnapshotDef {
    override val name = "edit_selected_models_label"

    override fun build(): Component {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("snapshot", network)).apply {
            preferredSize = Dimension(600, 400)
        }
        runBlocking {
            val neurons = (0 until 5).map { Neuron().apply { location = point(it * 40.0, 0.0) } }
            neurons.forEach { network.addNetworkModel(it, usePlacementManager = false) }
            network.addNetworkModel(Synapse(neurons[0], neurons[1]), usePlacementManager = false)
            network.addNetworkModel(Synapse(neurons[1], neurons[2]), usePlacementManager = false)
            network.addNetworkModel(Synapse(neurons[2], neurons[3]), usePlacementManager = false)
            network.addNetworkModel(NeuronArray(4).apply { location = point(0.0, 200.0) }, usePlacementManager = false)
            network.addNetworkModel(NetworkTextObject("note").apply { location = point(0.0, -150.0) }, usePlacementManager = false)
            val source = TensorLayer(TensorShape(4, 4))
            val convolutionTarget = TensorLayer(TensorShape(4, 4))
            val poolingTarget = TensorLayer(TensorShape(2, 2))
            network.addNetworkModels(source, convolutionTarget, poolingTarget)
            network.addNetworkModel(ConvolutionConnector(source, convolutionTarget, numFilters = 1))
            network.addNetworkModel(PoolingConnector(source, poolingTarget))
        }
        repeat(3) { SwingUtilities.invokeAndWait {} }

        val rows = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        }
        SwingUtilities.invokeAndWait { JDialog().apply { contentPane = panel; pack() } }
        val neurons = panel.filterScreenElements<NeuronNode>()
        val synapses = panel.filterScreenElements<SynapseNode>()
        val arrays = panel.filterScreenElements<NeuronArrayNode>()
        val texts = panel.filterScreenElements<TextNode>()
        val tensors = panel.filterScreenElements<TensorNode>()
        val connectors = panel.filterScreenElements<TensorConnectorNode>()
        val cases: List<Pair<String, List<ScreenElement>>> = listOf(
            "1 neuron" to neurons.take(1),
            "5 neurons" to neurons,
            "3 synapses" to synapses,
            "neurons + synapses" to neurons + synapses,
            "neurons + synapses + array" to neurons + synapses + arrays,
            "neurons + text" to neurons.take(2) + texts,
            "text only" to texts,
            "tensors with different shapes" to tensors,
            "convolution + pooling" to connectors,
            "connector and its box (count once)" to listOf(connectors.first(), connectors.first().interactionBox),
        )
        cases.forEach { (title, selection) ->
            panel.selectionManager.set(selection)
            repeat(3) { SwingUtilities.invokeAndWait {} }
            SwingUtilities.invokeAndWait {
                rows.add(JLabel(title).apply { border = BorderFactory.createEmptyBorder(8, 0, 2, 0) })
                val menu = panel.createSelectionEditMenu()
                rows.add(JLabel(menu.text + " ▸").apply { isEnabled = menu.isEnabled })
                val items = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    border = BorderFactory.createEmptyBorder(0, 20, 0, 0)
                }
                (0 until menu.itemCount).mapNotNull { menu.getItem(it) }.forEach { item ->
                    items.add(JMenuItem(item.text).apply { accelerator = item.accelerator })
                }
                rows.add(items)
            }
        }
        return rows
    }
}
