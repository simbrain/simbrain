package org.simbrain.util.uisnapshot

import net.miginfocom.swing.MigLayout
import org.simbrain.util.ResourceManager
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Renders a gallery of icons resolved through the real `ResourceManager.getSmallIcon` → `Icons`
 * path, at the canonical small size. Covers every icon referenced in the app, so it shows the
 * SVG-vs-raster migration state at a glance and doubles as a cross-theme icon regression target
 * (`-Ptheme=light|dark`).
 */
class IconGallerySnapshot : UiSnapshotDef {
    override val name = "icon_gallery"

    private val icons = listOf(
        // standard actions (migrated to SVG)
        "Save", "Open", "export", "import", "Extract", "Copy", "Cut", "Paste", "Undo", "Redo",
        "Play", "Stop", "Step", "Reset", "ZoomIn", "ZoomOut", "ZoomFitPage", "ZoomReset",
        "plus", "minus", "Prefs", "Tools", "Properties", "PenToSquare", "DocumentInfo", "Eraser",
        "Help", "Info", "Link", "chainIcon", "brokenChainIcon", "Up", "Down", "UpFull", "DownFull",
        "TangoIcons-GoNext", "TangoIcons-GoPrevious", "BarChart", "histogram", "PieChart",
        "TimeSeries", "ScatterIcon", "Table", "grid", "TableBold", "AddTableColumn",
        "DeleteTableColumn", "AddTableRow", "DeleteTableRow", "fill", "Fill_0", "Rand", "Rand_C",
        "AlignHorizontal", "AlignVertical", "SpaceHorizontal", "SpaceVertical", "Arrow", "Hand",
        "Sequence", "TestInput", "Text", "Terminal", "speaker", "World", "camera", "photo", "Trophy",
        // domain / special (still raster until later stages)
        "ActivationTool", "Clamp", "CubeShadow", "network_icon_black", "mouse_icon", "swiss_icon",
        "lambda", "PixelPlot", "RasterPlot", "Gauge", "GreenCheck", "RedX"
    ).map { "menu_icons/$it.png" }

    override fun build(): JPanel {
        val panel = JPanel(MigLayout("wrap 6, insets 16, gapx 20, gapy 12", "", ""))
        panel.background = Color.WHITE
        for (path in icons) {
            val cell = JPanel(MigLayout("insets 0, gapx 7, aligny center"))
            cell.isOpaque = false
            cell.add(JLabel(ResourceManager.getSmallIcon(path)))
            cell.add(JLabel(path.removePrefix("menu_icons/").removeSuffix(".png")))
            panel.add(cell)
        }
        return panel
    }
}
