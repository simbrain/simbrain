package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * The "Plot activations" submenu that neuron arrays, neuron collections, weight matrices, and the data
 * and text worlds all share, so the set of plot types offered there can be checked at a glance.
 */
class CoupledPlotMenuSnapshot : UiSnapshotDef {
    override val name = "coupled_plot_menu"

    override fun build(): Component {
        val workspace = SimbrainDesktop.workspace
        val networkComponent = NetworkComponent("Network")
        val neuronArray = NeuronArray(8)
        networkComponent.network.addNetworkModelAsync(neuronArray)
        workspace.addWorkspaceComponent(networkComponent)

        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val menu = SimbrainDesktop.actionManager.createCoupledPlotMenu(
                neuronArray.getProducer(NeuronArray::activationArray),
                "Neuron Array Activations",
                "Plot Activations"
            )
            // The popup only paints once realized, so render its real JMenuItems in a plain column.
            val column = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
                add(JLabel(menu.text).apply { border = BorderFactory.createEmptyBorder(0, 4, 6, 4) })
                menu.menuComponents.forEach { add(it) }
            }
            host = JFrame().apply {
                contentPane = JPanel(BorderLayout()).apply { add(column, BorderLayout.CENTER) }
                pack()
            }
        }
        return host
    }
}
