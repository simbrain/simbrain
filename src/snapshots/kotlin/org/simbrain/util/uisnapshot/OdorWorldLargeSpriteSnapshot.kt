package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * Close-up of enlarged sprites drawn through the pixel-exact painter: a scaled-up image sprite cut by the viewport
 * edge, and a procedural sprite turned to an oblique heading, both with even pixel grids. The mouse's whisker sensors
 * sit out at its scaled-up nose.
 */
class OdorWorldLargeSpriteSnapshot : UiSnapshotDef {

    override val name = "odor-world-large-sprite"

    override fun build(): Component {
        val component = OdorWorldComponent("Large sprites")
        val world = component.world
        world.isUseCameraCentering = false
        world.isObjectsBlockMovement = false
        world.tileMap.updateMapSize(20, 12)
        runBlocking {
            world.entityList.toList().forEach { it.delete() }
            world.addEntity(30, 60, EntityType.Swiss).apply { scale = 3.0 }
            world.addEntity(150, 60, EntityType.Isopod).apply {
                scale = 4.0
                heading = 30.0
            }
            world.addEntity(230, 60, EntityType.Mouse).apply {
                scale = 2.0
                addSensor(SmellSensor("Left", theta = 50.0, radius = 20.0))
                addSensor(SmellSensor("Right", theta = -50.0, radius = 20.0))
                isShowSensorsAndEffectors = true
            }
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait {
            panel = OdorWorldPanel(component, world)
            panel.preferredSize = Dimension(700, 300)
            panel.size = panel.preferredSize
            panel.doLayout()
            panel.autoZoom = false
            panel.scalingFactor = 2.7
            panel.canvas.camera.setViewOffset(-37.3, -21.6)
            panel.clearSelection()
        }
        repeat(3) { SwingUtilities.invokeAndWait { } }
        return panel
    }
}
