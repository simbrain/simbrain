package org.simbrain.util.uisnapshot

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Component
import java.awt.GridLayout
import javax.swing.JPanel

/**
 * The entity property editor for a continuous and a grid-mode mouse side by side; grid speed only shows on the
 * right.
 */
class OdorWorldEntityDialogSnapshot : UiSnapshotDef {

    override val name = "odor-world-entity-dialog"

    override fun build(): Component {
        val world = OdorWorld()
        val continuous = OdorWorldEntity(world, EntityType.Mouse)
        val grid = OdorWorldEntity(world, EntityType.Mouse).apply { movementMode = MovementMode.GRID }
        return JPanel(GridLayout(1, 2, 12, 0)).apply {
            add(AnnotatedPropertyEditor(continuous))
            add(AnnotatedPropertyEditor(grid))
        }
    }
}
