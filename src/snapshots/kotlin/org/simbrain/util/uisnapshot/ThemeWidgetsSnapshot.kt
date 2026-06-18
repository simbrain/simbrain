package org.simbrain.util.uisnapshot

import net.miginfocom.swing.MigLayout
import org.simbrain.util.Theme
import org.simbrain.util.widgets.SimbrainToggleButton
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Exercises the theme-derived chrome that must track a light/dark switch: [Theme.sectionBorder]
 * (header strip), [Theme.roundedCard], a muted label, and [SimbrainToggleButton] in its off /
 * selected states. Rendered under light and dark to confirm the borders and toggle backgrounds read
 * their colors live rather than baking light-mode grays.
 */
class ThemeWidgetsSnapshot : UiSnapshotDef {
    override val name = "theme_widgets"

    override fun build(): Component {
        lateinit var panel: JPanel
        SwingUtilities.invokeAndWait {
            panel = JPanel(MigLayout("wrap 1, fillx, insets 12", "[grow]")).apply {
                preferredSize = Dimension(360, 280)

                add(JPanel(MigLayout("wrap 1, insets 0")).apply {
                    border = Theme.sectionBorder("Section Header")
                    add(JLabel("Body label"))
                    add(JLabel("Muted label").apply { foreground = Theme.mutedText })
                }, "growx")

                add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    border = Theme.roundedCard(radius = 8, padding = 8)
                    add(JLabel("Rounded card"))
                }, "growx")

                add(JPanel(FlowLayout(FlowLayout.LEFT, 6, 6)).apply {
                    add(SimbrainToggleButton(text = "Off"))
                    add(SimbrainToggleButton(text = "On").apply { isSelected = true })
                }, "growx")
            }
        }
        return panel
    }
}
