package org.simbrain.util.uisnapshot

import org.simbrain.util.genericframe.GenericJInternalFrame
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JDesktopPane
import javax.swing.JFrame
import javax.swing.JInternalFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToolBar
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * A single focused [GenericJInternalFrame], so the active title-bar tint and the rounded
 * top-corner fill (the title-band seam fix) can be inspected — the two-frame pair snapshot can't
 * show the focused frame's title pane due to a harness selection quirk.
 */
class RoundedInternalFrameActiveSnapshot : UiSnapshotDef {
    override val name = "rounded_internal_frame_active"

    override fun build(): Component {
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val desktop = JDesktopPane().apply { preferredSize = Dimension(400, 300) }
            val content = JPanel(BorderLayout()).apply {
                add(JToolBar().apply {
                    isFloatable = false
                    add(JButton("Undo")); add(JButton("Redo")); addSeparator(); add(JButton("Zoom +"))
                }, BorderLayout.NORTH)
                add(JPanel().apply { background = Color.WHITE }, BorderLayout.CENTER)
                add(JLabel("0 iterations", SwingConstants.CENTER), BorderLayout.SOUTH)
            }
            val frame = GenericJInternalFrame().apply {
                title = "CNN"
                isClosable = true; isMaximizable = true; isIconifiable = true; isResizable = true
                contentPane = content
                isVisible = true
                setBounds(24, 24, 340, 240)
            }
            desktop.add(frame)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
            runCatching { (frame as JInternalFrame).isSelected = true }
        }
        return host
    }
}
