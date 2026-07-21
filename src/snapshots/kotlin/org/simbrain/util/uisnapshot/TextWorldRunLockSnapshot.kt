package org.simbrain.util.uisnapshot

import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.gui.TextWorldPanel
import java.awt.Component
import java.awt.Dimension

/**
 * The text world panel with the run lock engaged: the text is read-only and the bottom label
 * says why. This is how a generative-model document looks while the workspace is running.
 */
class TextWorldRunLockSnapshot : UiSnapshotDef {
    override val name = "text-world-run-lock"

    override fun build(): Component {
        val world = TextWorld()
        world.lockWhileRunning = true
        world.setTextNoEvent("The capital of France is Paris. The capital of Germany is")
        val panel = TextWorldPanel(world).apply {
            preferredSize = Dimension(420, 300)
        }
        panel.setRunLock(true)
        return panel
    }
}
