package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import org.simbrain.world.odorworld.editMenu
import org.simbrain.world.odorworld.insertMenu
import org.simbrain.world.odorworld.viewMenu
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
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * An odor world frame showing its menu bar and toolbar, with the items of each menu and both context menus laid
 * out in columns below it, so the File | Edit | Insert | View | Help organization can be checked at a glance.
 */
class OdorWorldMenusSnapshot : UiSnapshotDef {
    override val name = "odor_world_menus"

    override fun build(): Component {
        val workspace = Workspace()
        workspace.componentFactory.createWorkspaceComponent("Odor world")
        val component = workspace.componentList.filterIsInstance<OdorWorldComponent>().last()
        lateinit var host: JFrame

        fun column(title: String, items: List<Component>) = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            add(JLabel(title).apply { border = BorderFactory.createEmptyBorder(0, 4, 6, 4) })
            items.forEach { add(it) }
        }

        lateinit var gui: OdorWorldDesktopComponent
        lateinit var desktop: JDesktopPane
        SwingUtilities.invokeAndWait {
            val frame = GenericJInternalFrame("Odor world", true, true, true, true)
            gui = workspace.componentFactory.createGuiComponent(frame, component) as OdorWorldDesktopComponent
            frame.contentPane = gui
            frame.setBounds(16, 16, 420, 320)
            frame.isVisible = true
            desktop = JDesktopPane().apply {
                preferredSize = Dimension(1000, 360)
                add(frame)
            }
        }
        // Adding an entity after the panel exists gives it a canvas node to hang the entity popup on.
        val entity = runBlocking { component.world.addEntity() }
        repeat(3) { SwingUtilities.invokeAndWait {} }

        SwingUtilities.invokeAndWait {
            val panel = gui.worldPanel
            val entityPopup: JPopupMenu = panel.getEntityNode(entity).createContextMenu(panel)
            val columns = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(column("Edit", panel.editMenu.menuComponents.toList()))
                add(column("Insert", panel.insertMenu.menuComponents.toList()))
                add(column("View", panel.viewMenu.menuComponents.toList()))
                add(column("Canvas popup", panel.getContextMenu().components.toList()))
                add(column("Entity popup", entityPopup.components.toList()))
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
