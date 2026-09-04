package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * A 5x5 maze with two-tile cells, a grid-mode mouse, and cheese, as the actor-critic simulation sets it up.
 */
class OdorWorldMazeSnapshot : UiSnapshotDef {

    override val name = "odor-world-maze"

    override fun build(): Component {
        val component = OdorWorldComponent("Maze")
        val world = component.world
        world.wrapAround = false
        world.isObjectsBlockMovement = false
        world.isUseCameraCentering = false
        world.generateMaze(5, 5, cellSizeInTiles = 2, seed = 42L)
        runBlocking {
            world.addEntity(32, 32, EntityType.Swiss)
            world.addEntity(288, 288, EntityType.Mouse).apply {
                heading = 90.0
                isShowSensorsAndEffectors = false
                movementMode = MovementMode.GRID
            }
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait {
            panel = OdorWorldPanel(component, world)
            panel.preferredSize = Dimension(340, 380)
        }
        return panel
    }
}
