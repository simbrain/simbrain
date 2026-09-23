/** Shows the dedicated font chooser's family, style, size lists, and sample preview. */
package org.simbrain.util.uisnapshot

import org.simbrain.util.widgets.FontChooserPanel
import java.awt.Component
import java.awt.Font

class FontChooserSnapshot : UiSnapshotDef {
    override val name = "font_chooser"
    override fun build(): Component = FontChooserPanel(listOf(Font(Font.SANS_SERIF, Font.BOLD, 18)))
}
