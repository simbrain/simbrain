/**
 * Swing legend strip shared by chart panels that replace JFreeChart's in-chart legend, which is
 * painted pixels and so cannot host interactive controls. Each entry shows a swatch in the series'
 * plot color, the series name, and optionally a remove control that is only revealed while the
 * pointer is over the entry; the control keeps its slot when unrevealed so the legend never
 * reflows under the mouse. Entries can also act as visibility toggles: clicking the swatch or name
 * shows or hides the series, and a hidden entry renders with a hollow swatch and muted name.
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
import javax.swing.SwingUtilities

class ChartLegendPanel : JPanel(FlowLayout(FlowLayout.CENTER, ENTRY_HGAP, ENTRY_VGAP)) {

    init {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                revalidate()
                parent?.revalidate()
            }
        })
    }

    /**
     * One legend row. [paint] and [visible] are queried at paint time so swatches track the
     * renderer's current series colors and visibility; a null [onRemove] renders the entry without
     * a remove control, and a null [onToggleVisibility] leaves the swatch and name inert instead of
     * making them a show/hide toggle.
     */
    class Entry(
        val label: String,
        val paint: () -> Paint?,
        val onRemove: (() -> Unit)? = null,
        val visible: () -> Boolean = { true },
        val onToggleVisibility: (() -> Unit)? = null
    )

    fun setEntries(entries: List<Entry>) {
        removeAll()
        layout = FlowLayout(FlowLayout.CENTER, ENTRY_HGAP, ENTRY_VGAP)
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
        var rowHovered = false
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { isOpaque = false }

        val swatch = object : JComponent() {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.paint = entry.paint() ?: Theme.mutedText
                if (entry.visible()) {
                    g2.fillRoundRect(0, (height - 10) / 2, 10, 10, 3, 3)
                } else {
                    g2.drawRoundRect(0, (height - 10) / 2, 9, 9, 3, 3)
                }
            }
        }.apply { preferredSize = Dimension(10, 14) }
        row.add(swatch)

        val nameLabel = object : JLabel(entry.label) {
            // Nullable because the LaF queries this from the JLabel constructor, before any color is set
            override fun getForeground(): Color? =
                if (entry.visible()) super.getForeground() else Theme.mutedText
        }
        row.add(nameLabel)

        entry.onToggleVisibility?.let { onToggle ->
            val toggleListener = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    onToggle()
                    row.repaint()
                }
            }
            listOf<JComponent>(swatch, nameLabel).forEach {
                it.toolTipText = "Show or hide ${entry.label}"
                it.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                it.addMouseListener(toggleListener)
            }
        }

        entry.onRemove?.let { onRemove ->
            val removeLabel = object : JLabel("✕") {
                var hover = false
                // Painted transparent until the pointer is over the entry; it keeps its slot in the
                // row either way, so revealing it never shifts the legend layout under the mouse
                override fun getForeground(): Color = when {
                    !rowHovered -> TRANSPARENT
                    hover -> Theme.foreground
                    else -> Theme.mutedText
                }
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

            // Entering a child fires mouseExited on the row, so the reveal listener sits on the row
            // and every child, and an exit only unreveals once the pointer has truly left the row
            val revealListener = object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    if (!rowHovered) {
                        rowHovered = true
                        row.repaint()
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    val point = SwingUtilities.convertPoint(e.component, e.point, row)
                    if (!row.contains(point)) {
                        rowHovered = false
                        row.repaint()
                    }
                }
            }
            (listOf(row) + row.components.toList()).forEach { it.addMouseListener(revealListener) }
        }

        return row
    }

    companion object {
        /** Gap between legend entries; kept tight because each entry already trails its own
         * unrevealed remove slot, which reads as part of the spacing. */
        private const val ENTRY_HGAP = 6

        private const val ENTRY_VGAP = 2

        private val TRANSPARENT = Color(0, 0, 0, 0)
    }
}
