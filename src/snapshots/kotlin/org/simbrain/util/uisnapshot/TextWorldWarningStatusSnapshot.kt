/** Renders the text world status bar showing an action-needed warning, as the tiny language model does with no prompt. */
package org.simbrain.util.uisnapshot

import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.TextWorldStatus
import org.simbrain.world.textworld.gui.TextWorldPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

class TextWorldWarningStatusSnapshot : UiSnapshotDef {

    override val name = "text-world-warning-status"

    override fun build(): Component {
        val world = TextWorld()
        world.statusMessageProvider = {
            TextWorldStatus("Nothing to generate from. Enter a vocabulary token to begin.", warning = true)
        }
        lateinit var panel: TextWorldPanel
        SwingUtilities.invokeAndWait {
            panel = TextWorldPanel(world)
            panel.preferredSize = Dimension(400, 300)
        }
        return panel
    }
}
