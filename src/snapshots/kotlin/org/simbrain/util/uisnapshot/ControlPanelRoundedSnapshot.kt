package org.simbrain.util.uisnapshot

import org.simbrain.util.ControlPanelKt
import java.awt.Component
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JInternalFrame
import javax.swing.JTextField
import javax.swing.SwingUtilities

/**
 * Renders the production [ControlPanelKt] (used by ~15 sims) on a desktop, so its rounded corners
 * and the taller themed title bar can be checked against the square-cornered plain frame it used to
 * render as. Items are added straight to the panel's main pane rather than through the async
 * add* helpers, which post to the EDT and would not have run by snapshot time.
 */
class ControlPanelRoundedSnapshot : UiSnapshotDef {
    override val name = "rounded_control_panel"

    override fun build(): Component {
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val controlPanel = ControlPanelKt("Control Panel").apply {
                for (i in 1..4) {
                    mainPanel.addItem("Category $i", JTextField(12))
                    mainPanel.addItem(JButton("Save Image for Category $i"))
                }
                setBounds(24, 24, 280, 320)
                isVisible = true
            }
            val desktop = JDesktopPane().apply { preferredSize = Dimension(360, 400) }
            desktop.add(controlPanel)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
            runCatching { (controlPanel as JInternalFrame).isSelected = true }
        }
        return host
    }
}
