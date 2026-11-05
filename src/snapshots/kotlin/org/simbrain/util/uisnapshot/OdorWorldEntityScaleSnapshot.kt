package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * Entity types in columns at scales 0.5, 1, and 2 in rows, to check scaled bounds, captions, and image smoothing.
 */
class OdorWorldEntityScaleSnapshot : UiSnapshotDef {

    override val name = "odor-world-entity-scale"

    override fun build(): Component {
        val component = OdorWorldComponent("Scale")
        val world = component.world
        world.isUseCameraCentering = false
        world.isObjectsBlockMovement = false
        world.tileMap.updateMapSize(22, 17)
        val types = listOf(EntityType.Swiss, EntityType.Lily, EntityType.Marigold, EntityType.Mouse, EntityType.Nematode, EntityType.Cow)
        val scales = listOf(0.5, 1.0, 2.0)
        runBlocking {
            world.entityList.toList().forEach { it.delete() }
            scales.forEachIndexed { row, scale ->
                types.forEachIndexed { col, type ->
                    world.addEntity(60 + col * 110, 50 + row * 170, type).apply {
                        this.scale = scale
                        caption = "$scale"
                        isShowSensorsAndEffectors = false
                    }
                }
            }
        }
        lateinit var panel: OdorWorldPanel
        SwingUtilities.invokeAndWait {
            panel = OdorWorldPanel(component, world)
            panel.preferredSize = Dimension(700, 560)
        }
        return panel
    }
}
