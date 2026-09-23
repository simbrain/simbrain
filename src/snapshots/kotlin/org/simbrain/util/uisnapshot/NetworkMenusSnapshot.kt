package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.actionMenu
import org.simbrain.network.gui.arrangeMenu
import org.simbrain.network.gui.connectMenu
import org.simbrain.network.gui.creatContextMenu
import org.simbrain.network.gui.createNeuronContextMenu
import org.simbrain.network.gui.editMenu
import org.simbrain.network.gui.insertMenu
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * A network frame showing its menu bar, with the items of the Edit, Insert, Connect, Arrange and Actions menus and
 * the canvas and neuron popups laid out in columns below it, so the menu organization can be checked at a glance.
 */
class NetworkMenusSnapshot : UiSnapshotDef {
    override val name = "network_menus"

    override fun build(): Component {
        val workspace = Workspace()
        workspace.componentFactory.createWorkspaceComponent("Network")
        val component = workspace.componentList.filterIsInstance<NetworkComponent>().last()
        lateinit var host: JFrame

        fun column(title: String, items: List<Component>) = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            add(JLabel(title).apply { border = BorderFactory.createEmptyBorder(0, 4, 6, 4) })
            items.forEach { add(it) }
        }

        lateinit var gui: NetworkDesktopComponent
        lateinit var desktop: JDesktopPane
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Network", true, true, true, true)
            gui = workspace.componentFactory.createGuiComponent(frame, component) as NetworkDesktopComponent
            frame.contentPane = gui
            frame.setBounds(16, 16, 700, 200)
            frame.isVisible = true
            desktop = JDesktopPane().apply {
                preferredSize = Dimension(1900, 240)
                add(frame)
            }
        }
        val neuron = Neuron()
        runBlocking { component.network.addNetworkModel(neuron) }
        repeat(3) { SwingUtilities.invokeAndWait {} }

        SwingUtilities.invokeAndWait {
            val panel = gui.networkPanel
            val columns = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(column("Edit", panel.editMenu.menuComponents.toList()))
                add(column("Insert", panel.insertMenu.menuComponents.toList()))
                add(column("Connect", panel.connectMenu.menuComponents.toList()))
                add(column("Arrange", panel.arrangeMenu.menuComponents.toList()))
                add(column("Actions", panel.actionMenu.menuComponents.toList()))
                add(column("Canvas popup", panel.creatContextMenu().components.toList()))
                add(column("Neuron popup", panel.createNeuronContextMenu(neuron).components.toList()))
            }
            host = JFrame().apply {
                contentPane = JPanel(BorderLayout()).apply {
                    add(desktop, BorderLayout.NORTH)
                    add(columns, BorderLayout.CENTER)
                }
                pack()
            }
        }
        repeat(3) { SwingUtilities.invokeAndWait {} }
        return host
    }
}
