/**
 * Swing legend strip shared by chart panels that replace JFreeChart's in-chart legend, which is
 * painted pixels and so cannot host interactive controls. Each entry shows a swatch in the series'
 * plot color, the series name, and optionally a muted remove control that brightens on hover.
 * Owning panels rebuild the strip via [setEntries] when their series change.
 */
package org.simbrain.plot

import org.simbrain.util.Theme
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ChartLegendPanel : JPanel(FlowLayout(FlowLayout.CENTER, 12, 2)) {

    init {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                revalidate()
                parent?.revalidate()
            }
        })
    }

    /**
     * One legend row. [paint] is queried at paint time so swatches track the renderer's current
     * series colors; a null [onRemove] renders the entry without a remove control.
     */
    class Entry(
        val label: String,
        val paint: () -> Paint?,
        val onRemove: (() -> Unit)? = null
    )

    fun setEntries(entries: List<Entry>) {
        removeAll()
        layout = FlowLayout(FlowLayout.CENTER, 12, 2)
        entries.forEach { add(entryComponent(it)) }
        revalidate()
        repaint()
    }

    /**
     * Reports the height of every wrapped row at the current width.
     *
     * FlowLayout wraps components while laying out but otherwise reports a one-row preferred size,
     * which would cause the parent plot layout to clip later legend rows.
     */
    override fun getPreferredSize(): Dimension {
        val fallback = super.getPreferredSize()
        val availableWidth = width.takeIf { it > 0 } ?: parent?.width?.takeIf { it > 0 } ?: return fallback
        val flowLayout = layout as? FlowLayout ?: return fallback
        val maxRowWidth = availableWidth - insets.left - insets.right - flowLayout.hgap * 2
        if (maxRowWidth <= 0) return fallback

        var rowWidth = 0
        var rowHeight = 0
        var totalHeight = flowLayout.vgap
        components.filter { it.isVisible }.forEach { component ->
            val preferred = component.preferredSize
            if (rowWidth > 0 && rowWidth + flowLayout.hgap + preferred.width > maxRowWidth) {
                totalHeight += rowHeight + flowLayout.vgap
                rowWidth = 0
                rowHeight = 0
            }
            rowWidth += if (rowWidth == 0) preferred.width else flowLayout.hgap + preferred.width
            rowHeight = maxOf(rowHeight, preferred.height)
        }
        if (rowWidth > 0) totalHeight += rowHeight + flowLayout.vgap
        return Dimension(availableWidth, totalHeight + insets.top + insets.bottom)
    }

    override fun getMinimumSize(): Dimension = preferredSize

    private fun entryComponent(entry: Entry): JComponent {
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { isOpaque = false }

        val swatch = object : JComponent() {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.paint = entry.paint() ?: Theme.mutedText
                g2.fillRoundRect(0, (height - 10) / 2, 10, 10, 3, 3)
            }
        }.apply { preferredSize = Dimension(10, 14) }
        row.add(swatch)

        row.add(JLabel(entry.label))

        entry.onRemove?.let { onRemove ->
            val removeLabel = object : JLabel("✕") {
                var hover = false
                override fun getForeground(): Color = if (hover) Theme.foreground else Theme.mutedText
            }
            removeLabel.toolTipText = "Remove ${entry.label}"
            removeLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            removeLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    removeLabel.hover = true
                    removeLabel.repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    removeLabel.hover = false
                    removeLabel.repaint()
                }

                override fun mouseClicked(e: MouseEvent) {
                    onRemove()
                }
            })
            row.add(removeLabel)
        }

        return row
    }
}
