/**
 * Entity movement reaches odor world nodes through dirty marks and one per-frame sync pass rather than an EDT task
 * per move: nodes still follow their entities, bursts of moves coalesce, a world update never waits on the EDT,
 * trails keep every world update's position even when frames are skipped, and nodes that leave the canvas stop
 * listening.
 */
package org.simbrain.world.odorworld

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.plot.awaitUntil
import org.simbrain.util.UiWork
import org.simbrain.util.countEdtTasks
import org.simbrain.util.finishesWhileEdtIsBlocked
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.geom.PathIterator
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities
import kotlin.math.cos
import kotlin.math.sin

class EntityNodeSyncTest {

    private fun panelFor(world: OdorWorld, component: OdorWorldComponent): OdorWorldPanel {
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait { panel = OdorWorldPanel(component, world) }
        return panel
    }

    private fun holdEdt(): CountDownLatch {
        val release = CountDownLatch(1)
        val held = CountDownLatch(1)
        SwingUtilities.invokeLater {
            held.countDown()
            release.await()
        }
        held.await()
        return release
    }

    @Test
    fun `an entity's node follows it after the sync pass`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        val panel = panelFor(world, component)
        val mouse = world.addEntity(100.0, 100.0, EntityType.Mouse)
        val node = panel.getEntityNode(mouse)

        mouse.location = point(150.0, 120.0)
        UiWork.awaitIdle()

        assertEquals(150.0, node.offset.x)
        assertEquals(120.0, node.offset.y)
    }

    @Test
    fun `a burst of moves costs a few edt tasks, not one each`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        val panel = panelFor(world, component)
        val mouse = world.addEntity(100.0, 100.0, EntityType.Mouse)
        val node = panel.getEntityNode(mouse)
        UiWork.awaitIdle()

        val tasks = countEdtTasks {
            val release = holdEdt()
            repeat(1000) { mouse.location = point(100.0 + it * 0.2, 100.0) }
            release.countDown()
            // Sleep rather than poll, so only the work the moves caused is counted
            Thread.sleep(200)
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "1000 moves posted $tasks EDT tasks")
        assertEquals(mouse.x, node.offset.x)
    }

    @Test
    fun `a world update does not wait for the edt`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        panelFor(world, component)
        world.addEntity(100.0, 100.0, EntityType.Mouse)

        assertTrue(finishesWhileEdtIsBlocked { repeat(20) { world.update() } })
    }

    @Test
    fun `a trail keeps every world update's position when the edt falls behind`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        val panel = panelFor(world, component)
        val mouse = world.addEntity(160.0, 160.0, EntityType.Mouse).apply {
            drawTrailWithoutRunningWorkspace = true
            isShowTrail = true
        }
        val node = panel.getEntityNode(mouse)
        UiWork.awaitIdle()

        val updates = 60
        val finished = finishesWhileEdtIsBlocked {
            repeat(updates) { i ->
                val angle = i * 2 * Math.PI / updates
                mouse.location = point(160.0 + 60 * cos(angle), 160.0 + 60 * sin(angle))
                world.update()
            }
        }
        UiWork.awaitIdle()
        assertTrue(finished, "world updates waited on the EDT")

        val iterator = node.trail.pathReference.getPathIterator(null)
        var lines = 0
        val coords = DoubleArray(6)
        while (!iterator.isDone) {
            if (iterator.currentSegment(coords) == PathIterator.SEG_LINETO) lines++
            iterator.next()
        }
        assertTrue(lines >= updates - 1, "a circle of $updates world updates drew $lines trail segments")
    }

    @Test
    fun `a deleted entity's node stops listening`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        val panel = panelFor(world, component)
        val mouse = world.addEntity(100.0, 100.0, EntityType.Mouse)
        val node = panel.getEntityNode(mouse)

        mouse.delete()

        awaitUntil { !node.nodeScope.isActive }
        assertNull(node.parent)
    }

    @Test
    fun `entity nodes rebuilt with the tile map leave the old ones disposed`() = runBlocking {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        val panel = panelFor(world, component)
        val mouse = world.addEntity(100.0, 100.0, EntityType.Mouse)
        val original = panel.getEntityNode(mouse)

        world.events.tileMapChanged.fire()

        awaitUntil { panel.getEntityNode(mouse) !== original }
        assertFalse(original.nodeScope.isActive, "a replaced node still listens to its entity")
        mouse.location = point(130.0, 110.0)
        UiWork.awaitIdle()
        assertEquals(130.0, panel.getEntityNode(mouse).offset.x)
    }
}
