/** Renders the complete text/font dialog for window sizing checks. */
package org.simbrain.util.uisnapshot

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.dialogs.text.TextDialog
import java.awt.Component

class TextDialogSnapshot : UiSnapshotDef {
    override val name = "text_dialog"

    override fun build(): Component {
        val textObject = NetworkTextObject("Sample").apply {
            fontName = "SansSerif"
            fontSize = 18
            isBold = true
        }
        return TextDialog(listOf(textObject))
    }
}
