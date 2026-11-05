package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.PNode
import org.piccolo2d.extras.handles.PHandle
import org.simbrain.network.gui.nodes.NodeHandle
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.gui.EntityNode
import java.awt.Component
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.SwingUtilities

/**
 * Resize handles on a selection of entities, from a tiny isopod to a cow, with a drag in progress on the cheese's
 * bottom right handle, which scales the whole selection and shows the readout.
 */
class OdorWorldResizeHandlesSnapshot : UiSnapshotDef {

    override val name = "odor-world-resize-handles"

    override fun build(): Component {
        val component = OdorWorldComponent("Resize")
        val world = component.world
        world.isUseCameraCentering = false
        world.isObjectsBlockMovement = false
        world.tileMap.updateMapSize(14, 9)
        val entities = runBlocking {
            world.entityList.toList().forEach { it.delete() }
            listOf(
                world.addEntity(60, 70, EntityType.Swiss),
                world.addEntity(160, 70, EntityType.Isopod),
                world.addEntity(270, 90, EntityType.Cow),
                world.addEntity(380, 90, EntityType.Mouse),
            ).onEach { it.isShowSensorsAndEffectors = false }
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait {
            panel = OdorWorldPanel(component, world)
            panel.preferredSize = Dimension(700, 440)
            panel.size = panel.preferredSize
            panel.doLayout()
            panel.autoZoom = false
            panel.scalingFactor = 1.5
            panel.canvas.camera.setViewOffset(0.0, 0.0)
        }
        SwingUtilities.invokeAndWait {
            val nodes = panel.canvas.layer.childrenReference.filterIsInstance<EntityNode>()
            panel.selectionManager.selection = nodes.map<EntityNode, PNode> { it }.toMutableSet()
        }
        SwingUtilities.invokeAndWait { }
        SwingUtilities.invokeAndWait {
            val swiss = panel.canvas.layer.childrenReference.filterIsInstance<EntityNode>().first { it.entity === entities[0] }
            val handle = swiss.getAllNodes(null, null)
                .filter { it is PHandle && it !is NodeHandle }
                .maxBy { (it as PNode).globalFullBounds.let { b -> b.centerX + b.centerY } } as PNode
            val c = handle.globalFullBounds.center2D
            val from = panel.canvas.camera.viewToLocal(Point2D.Double(c.x, c.y))
            fun send(id: Int, x: Double, y: Double, mods: Int) = panel.canvas.dispatchEvent(
                MouseEvent(panel.canvas, id, 0L, mods, x.toInt(), y.toInt(), 1, false, MouseEvent.BUTTON1)
            )
            send(MouseEvent.MOUSE_MOVED, from.x, from.y, 0)
            send(MouseEvent.MOUSE_PRESSED, from.x, from.y, InputEvent.BUTTON1_DOWN_MASK)
            for (i in 1..4) send(MouseEvent.MOUSE_DRAGGED, from.x + 4 * i, from.y + 4 * i, InputEvent.BUTTON1_DOWN_MASK)
        }
        // Size changes reach the nodes through queued Swing tasks; let them all run before the frame is captured
        repeat(5) {
            Thread.sleep(50)
            SwingUtilities.invokeAndWait { }
        }
        return panel
    }
}
