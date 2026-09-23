/** Renders the creation variant of the shared APE text and font dialog. */
package org.simbrain.util.uisnapshot

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.dialogs.text.TextDialog
import java.awt.Component

class TextCreationDialogSnapshot : UiSnapshotDef {
    override val name = "text_creation_dialog"
    override fun build(): Component = TextDialog(listOf(NetworkTextObject()), titleName = "Add Text")
}
