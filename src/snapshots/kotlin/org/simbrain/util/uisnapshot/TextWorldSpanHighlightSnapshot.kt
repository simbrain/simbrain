package org.simbrain.util.uisnapshot

import org.simbrain.world.textworld.DocumentStructureDisplay
import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.gui.TextWorldPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

/**
 * The LLM document mid-prefill: the model-driven span highlight sweeping the text while the
 * status bar reports the phase, configured the way the LFM2 simulation configures its
 * document world.
 */
class TextWorldSpanHighlightSnapshot : UiSnapshotDef {

    override val name = "text-world-span-highlight"

    override fun build(): Component {
        val world = TextWorld()
        world.documentStructureDisplay = DocumentStructureDisplay.CONVERSATION_FOCUS
        world.showTokenBoundaries = false
        world.text = "<|startoftext|>Here is a brief two-paragraph parable:\n\n" +
            "In a small village, there lived a wise old man named Elder."
        world.statusMessageProvider = { "Finished — edit the text to continue" }
        world.tokenCountLabelProvider = { "147 used / 365 remaining" }
        val start = world.text.indexOf("village")
        world.setHighlightSpan(intArrayOf(start, start + "village".length))
        lateinit var panel: TextWorldPanel
        SwingUtilities.invokeAndWait {
            panel = TextWorldPanel(world)
            panel.preferredSize = Dimension(400, 300)
            panel.updateHighlights()
        }
        return panel
    }
}
