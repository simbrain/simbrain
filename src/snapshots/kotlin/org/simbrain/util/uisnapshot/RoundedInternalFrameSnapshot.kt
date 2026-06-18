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
 * Renders the production [GenericJInternalFrame] (rounded corners + soft shadow) on a desktop, with
 * a focused frame overlapping an unfocused one so the rounding and shadow can be judged together.
 */
class RoundedInternalFrameSnapshot : UiSnapshotDef {
    override val name = "rounded_internal_frame"

    private fun frame(title: String, footer: String): GenericJInternalFrame {
        val content = JPanel(BorderLayout()).apply {
            add(JToolBar().apply {
                isFloatable = false
                add(JButton("Undo")); add(JButton("Redo")); addSeparator(); add(JButton("Zoom +"))
            }, BorderLayout.NORTH)
            add(JPanel().apply { background = Color.WHITE }, BorderLayout.CENTER)
            add(JLabel(footer, SwingConstants.CENTER), BorderLayout.SOUTH)
        }
        return GenericJInternalFrame().apply {
            this.title = title
            isClosable = true; isMaximizable = true; isIconifiable = true; isResizable = true
            contentPane = content
            isVisible = true
        }
    }

    override fun build(): Component {
        lateinit var host: JFrame
        SwingUtilities.invokeAndWait {
            val desktop = JDesktopPane().apply { preferredSize = Dimension(560, 380) }
            val back = frame("Odor World", "inactive frame").apply { setBounds(24, 24, 300, 230) }
            val front = frame("CNN", "0 iterations").apply { setBounds(230, 120, 300, 230) }
            desktop.add(back)
            desktop.add(front)
            host = JFrame().apply { contentPane = desktop }
            host.pack()
            runCatching { (front as JInternalFrame).isSelected = true }
        }
        return host
    }
}
