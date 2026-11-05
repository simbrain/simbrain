/**
 * Resize handles on a live odor world panel: they appear only while an entity's body is selected, and a mouse drag on
 * one goes to the handle rather than to the panel's move and lasso handler.
 */
package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PNode
import org.piccolo2d.extras.handles.PHandle
import org.simbrain.network.gui.nodes.NodeHandle
import org.simbrain.util.onEdt
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

class EntityResizeHandlesTest {

    private class Fixture(val panel: OdorWorldPanel, val entities: List<OdorWorldEntity>) {
        fun node(entity: OdorWorldEntity) = panel.canvas.layer.childrenReference
            .filterIsInstance<EntityNode>()
            .single { it.entity === entity }

        fun resizeHandles(entity: OdorWorldEntity) = onEdt {
            node(entity).getAllNodes(null, null).filter { it is PHandle && it !is NodeHandle }.map { it as PHandle }
        }

        fun select(vararg selected: OdorWorldEntity) {
            onEdt { panel.selectionManager.selection = selected.map<OdorWorldEntity, PNode> { node(it) }.toMutableSet() }
            onEdt { }
        }
    }

    private fun fixture(vararg locations: Point2D): Fixture {
        val component = OdorWorldComponent("Resize")
        val world = component.world
        world.isUseCameraCentering = false
        world.isObjectsBlockMovement = false
        world.tileMap.updateMapSize(20, 20)
        val entities = runBlocking {
            world.entityList.toList().forEach { it.delete() }
            locations.map { world.addEntity(it.x.toInt(), it.y.toInt(), EntityType.Swiss) }
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait {
            panel = OdorWorldPanel(component, world)
            panel.setSize(500, 500)
            panel.doLayout()
            panel.canvas.setSize(500, 500)
            panel.selectionManager.clear()
        }
        onEdt { }
        onEdt {
            panel.autoZoom = false
            panel.scalingFactor = 1.0
        }
        return Fixture(panel, entities)
    }

    private fun mouse(panel: OdorWorldPanel, id: Int, p: Point2D, modifiers: Int) {
        SwingUtilities.invokeAndWait {
            panel.canvas.dispatchEvent(
                MouseEvent(
                    panel.canvas, id, System.currentTimeMillis(), modifiers, p.x.toInt(), p.y.toInt(), 1, false,
                    MouseEvent.BUTTON1
                )
            )
        }
    }

    private fun drag(panel: OdorWorldPanel, from: Point2D, to: Point2D) {
        val down = InputEvent.BUTTON1_DOWN_MASK
        mouse(panel, MouseEvent.MOUSE_MOVED, from, 0)
        mouse(panel, MouseEvent.MOUSE_PRESSED, from, down)
        val steps = 5
        for (i in 1..steps) {
            val t = i.toDouble() / steps
            mouse(panel, MouseEvent.MOUSE_DRAGGED, Point2D.Double(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t), down)
        }
        mouse(panel, MouseEvent.MOUSE_RELEASED, to, 0)
        onEdt { }
    }

    private fun handleCenterOnCanvas(panel: OdorWorldPanel, handle: PHandle): Point2D = onEdt {
        val center = handle.globalFullBounds.center2D
        val p = panel.canvas.camera.viewToLocal(Point2D.Double(center.x, center.y))
        Point2D.Double(p.x.roundToInt().toDouble(), p.y.roundToInt().toDouble())
    }

    private fun bottomRightHandle(f: Fixture, entity: OdorWorldEntity) =
        f.resizeHandles(entity).maxBy { onEdt { it.globalFullBounds.centerX + it.globalFullBounds.centerY } }

    @Test
    fun `selecting an entity shows four handles and deselecting removes them`() {
        val f = fixture(Point2D.Double(150.0, 150.0))
        val entity = f.entities.single()
        assertEquals(0, f.resizeHandles(entity).size)

        f.select(entity)
        assertEquals(4, f.resizeHandles(entity).size)

        onEdt { f.panel.clearSelection() }
        onEdt { }
        assertEquals(0, f.resizeHandles(entity).size)
    }

    @Test
    fun `dragging the bottom right handle outward grows the entity about its center`() {
        val f = fixture(Point2D.Double(150.0, 150.0))
        val entity = f.entities.single()
        f.select(entity)
        val from = handleCenterOnCanvas(f.panel, bottomRightHandle(f, entity))
        val d = 16.0

        drag(f.panel, from, Point2D.Double(from.x + d, from.y + d))

        assertEquals(2.0, entity.scale, 1e-6)
        assertEquals(150.0, entity.x, 1e-9)
        assertEquals(150.0, entity.y, 1e-9)
    }

    @Test
    fun `dragging one handle scales every selected entity by the same ratio`() {
        val f = fixture(Point2D.Double(100.0, 100.0), Point2D.Double(250.0, 250.0))
        val (a, b) = f.entities
        a.scale = 0.5
        onEdt { }
        f.select(a, b)
        val from = handleCenterOnCanvas(f.panel, bottomRightHandle(f, b))
        val d = 8.0

        drag(f.panel, from, Point2D.Double(from.x + d, from.y + d))

        assertEquals(1.5, b.scale, 1e-6)
        assertEquals(0.75, a.scale, 1e-6)
        assertTrue(f.panel.selectedEntityModels.containsAll(listOf(a, b)))
    }

    @Test
    fun `double clicking a handle resets the selection to native size`() {
        val f = fixture(Point2D.Double(150.0, 150.0))
        val entity = f.entities.single()
        entity.scale = 2.5
        onEdt { }
        f.select(entity)
        val at = handleCenterOnCanvas(f.panel, bottomRightHandle(f, entity))

        SwingUtilities.invokeAndWait {
            for (clicks in 1..2) {
                for (id in listOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
                    val mods = if (id == MouseEvent.MOUSE_PRESSED) InputEvent.BUTTON1_DOWN_MASK else 0
                    f.panel.canvas.dispatchEvent(
                        MouseEvent(f.panel.canvas, id, 0L, mods, at.x.toInt(), at.y.toInt(), clicks, false, MouseEvent.BUTTON1)
                    )
                }
            }
        }
        onEdt { }

        assertEquals(1.0, entity.scale)
    }
}
