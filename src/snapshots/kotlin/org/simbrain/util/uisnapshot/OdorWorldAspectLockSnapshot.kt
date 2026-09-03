package org.simbrain.util.uisnapshot

import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.AspectLockingDesktopManager
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JInternalFrame
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Two odor worlds with a 4:1 map, both dragged to the same mismatched frame size through the production
 * [AspectLockingDesktopManager] and then zoomed all the way out. The locked frame should snap to the world's
 * shape and show the whole map; the unlocked frame keeps the dragged size and clips the map horizontally.
 */
class OdorWorldAspectLockSnapshot : UiSnapshotDef {
    override val name = "odor_world_aspect_lock"

    override fun build(): Component {
        val workspace = Workspace()
        val manager = AspectLockingDesktopManager()
        lateinit var host: JFrame
        lateinit var desktop: JDesktopPane
        val frames = mutableListOf<Pair<JInternalFrame, OdorWorldDesktopComponent>>()

        fun createWorld(locked: Boolean): OdorWorldComponent {
            workspace.componentFactory.createWorkspaceComponent("Odor world")
            val component = workspace.componentList.filterIsInstance<OdorWorldComponent>().last()
            component.world.tileMap.updateMapSize(40, 10)
            component.world.lockAspectRatio = locked
            // Spread the entities along the full map width so horizontal clipping is visible.
            component.world.entityList.forEachIndexed { i, entity ->
                entity.setLocation(60 + i * 1150 / (component.world.entityList.size - 1), 80 + (i % 2) * 140)
            }
            return component
        }
        val components = listOf(createWorld(locked = true), createWorld(locked = false))

        fun addWorld(title: String, component: OdorWorldComponent, x: Int) {
            val frame = GenericJInternalFrame(title, true, true, true, true)
            val gui = workspace.componentFactory.createGuiComponent(frame, component) as OdorWorldDesktopComponent
            frame.contentPane = gui
            frame.setBounds(x, 16, 400, 300)
            frame.isVisible = true
            desktop.add(frame)
            frames += frame to gui
        }

        SwingUtilities.invokeAndWait {
            desktop = JDesktopPane().apply {
                desktopManager = manager
                preferredSize = Dimension(1000, 380)
            }
            addWorld("Locked to world ratio", components[0], x = 16)
            addWorld("Unlocked", components[1], x = 510)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
        }
        // Let the deferred fit-to-world sizing from construction settle before simulating the drag.
        repeat(3) { SwingUtilities.invokeAndWait {} }

        SwingUtilities.invokeAndWait {
            frames.forEach { (frame, gui) ->
                manager.beginResizingFrame(frame, SwingConstants.SOUTH_EAST)
                manager.resizeFrame(frame, frame.x, frame.y, 470, 330)
                manager.endResizingFrame(frame)
                desktop.validate()
                gui.worldPanel.canvas.scale(0.01)
            }
        }
        repeat(2) { SwingUtilities.invokeAndWait {} }
        return host
    }
}
