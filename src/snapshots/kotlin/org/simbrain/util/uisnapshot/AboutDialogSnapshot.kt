package org.simbrain.util.uisnapshot

import org.simbrain.workspace.gui.buildAboutDialog
import java.awt.Component

class AboutDialogSnapshot : UiSnapshotDef {
    override val name = "about_dialog"

    override fun build(): Component = buildAboutDialog(null)
}
