package org.simbrain.util.uisnapshot

import org.simbrain.network.gui.WandPalette
import org.simbrain.network.gui.WandPalettePanel
import java.awt.Component
import javax.swing.SwingUtilities

class WandPalettePanelSnapshot : UiSnapshotDef {
    override val name = "wand_palette_panel"

    override fun build(): Component {
        val palette = WandPalette.createDefault()
        lateinit var panel: Component
        SwingUtilities.invokeAndWait {
            panel = WandPalettePanel(palette)
        }
        return panel
    }
}
