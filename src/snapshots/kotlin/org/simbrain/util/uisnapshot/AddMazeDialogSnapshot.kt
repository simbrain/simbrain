package org.simbrain.util.uisnapshot

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.gui.MazeSettings
import java.awt.Component

/**
 * The property editor behind Insert > Add maze..., with the seed field disabled until "Use seed" is checked.
 */
class AddMazeDialogSnapshot : UiSnapshotDef {

    override val name = "add-maze-dialog"

    override fun build(): Component = AnnotatedPropertyEditor(MazeSettings(OdorWorld()))
}
