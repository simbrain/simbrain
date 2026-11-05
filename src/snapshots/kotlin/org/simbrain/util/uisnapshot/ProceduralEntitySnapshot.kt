package org.simbrain.util.uisnapshot

import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.ProceduralEntityImages
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.*
import javax.swing.JPanel

/**
 * Each animation frame of the procedurally drawn entities, enlarged and at native size, on a light ground.
 */
class ProceduralEntitySnapshot : UiSnapshotDef {

    override val name = "procedural-entities"

    private val types = listOf(EntityType.Isopod, EntityType.Pillbug, EntityType.Nematode)
    private val zoom = 8

    override fun build(): Component {
        val world = OdorWorld()
        val entities = types.map { OdorWorldEntity(world, it) }
        val rowHeight = types.maxOf { it.height } * zoom + 20
        return object : JPanel() {
            init {
                preferredSize = Dimension(4 * (types.maxOf { it.width } * zoom + 20) + 140, rowHeight * types.size + 20)
                background = Color(214, 222, 196)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                entities.forEachIndexed { row, entity ->
                    val type = entity.entityType
                    val y = 10 + row * rowHeight
                    var x = 10
                    repeat(type.numFrames) { frame ->
                        val image = ProceduralEntityImages.getImage(entity, 0, frame)!!
                        g2.drawImage(image, x, y, type.width * zoom, type.height * zoom, null)
                        x += type.width * zoom + 20
                    }
                    val native = ProceduralEntityImages.getImage(entity, 0, 0)!!
                    g2.drawImage(native, x, y, type.width, type.height, null)
                    g2.drawImage(native, x + 40, y, type.width * 2, type.height * 2, null)
                }
            }
        }
    }
}
