/**
 * An odor world panel subscribes to the current tile map once however often the tile map changes, and a panel whose
 * view ends leaves no subscriptions on its world, which outlives it.
 */
package org.simbrain.world.odorworld

import kotlinx.coroutines.cancel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.onEdt
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.subscriberTotal
import javax.swing.SwingUtilities

class OdorWorldPanelLifetimeTest {

    private fun panelFor(world: OdorWorld, component: OdorWorldComponent): OdorWorldPanel {
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait { panel = OdorWorldPanel(component, world) }
        return panel
    }

    /** Fires a tile map change and lets the panel's EDT handler for it run. */
    private fun changeTileMap(world: OdorWorld) {
        world.events.tileMapChanged.fire()
        onEdt { }
    }

    @Test
    fun `repeated tile map changes keep one set of handlers on the tile map`() {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        panelFor(world, component)
        val withoutPanelHandlers = world.tileMap.events.subscriberTotal()
        changeTileMap(world)
        val once = world.tileMap.events.subscriberTotal()
        assertTrue(once > withoutPanelHandlers)

        repeat(3) { changeTileMap(world) }

        assertEquals(once, world.tileMap.events.subscriberTotal())
    }

    @Test
    fun `a panel whose view ends leaves no subscriptions on its world or tile map`() {
        val component = OdorWorldComponent("World")
        val world = component.world.apply { tileMap = TileMap(20, 20) }
        fun total() = world.events.subscriberTotal() + world.tileMap.events.subscriberTotal()
        val withoutPanel = total()
        val panel = panelFor(world, component)
        changeTileMap(world)
        assertTrue(total() > withoutPanel)

        onEdt { panel.viewScope.cancel() }

        assertEquals(withoutPanel, total())
    }
}
