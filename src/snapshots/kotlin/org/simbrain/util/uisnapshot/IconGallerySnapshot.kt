package org.simbrain.util.uisnapshot

import net.miginfocom.swing.MigLayout
import org.simbrain.util.ResourceManager
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Renders a gallery of icons resolved through the real `ResourceManager.getSmallIcon` → `Icons`
 * path, at the canonical small size. Useful for eyeballing the SVG-vs-raster migration and as a
 * cross-theme icon regression target (`-Ptheme=light|dark`).
 */
class IconGallerySnapshot : UiSnapshotDef {
    override val name = "icon_gallery"

    private val icons = listOf(
        "Save", "Open", "Copy", "Cut", "Paste", "Undo", "Redo", "Play", "Stop", "Step",
        "ZoomIn", "ZoomOut", "ZoomFitPage", "Prefs", "Tools", "Eraser", "plus", "minus", "Help", "Info",
        "Table", "World", "camera", "BarChart", "PieChart", "TimeSeries", "Link", "brokenChainIcon",
        "Properties", "Reset"
    ).map { "menu_icons/$it.png" }

    override fun build(): JPanel {
        val panel = JPanel(MigLayout("wrap 5, insets 16, gapx 22, gapy 14", "", ""))
        panel.background = Color.WHITE
        for (path in icons) {
            val cell = JPanel(MigLayout("insets 0, gapx 8, aligny center"))
            cell.isOpaque = false
            cell.add(JLabel(ResourceManager.getSmallIcon(path)))
            cell.add(JLabel(path.removePrefix("menu_icons/").removeSuffix(".png")))
            panel.add(cell)
        }
        return panel
    }
}
