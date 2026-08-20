/**
 * Rendering canary for the text world's Unicode coverage: emoji, CJK, RTL and combining
 * characters — all of which language models emit — shown in the [SimbrainTextPane] the text
 * world uses (including role coloring), with the retired RSyntaxTextArea rendering and a plain
 * JTextArea as contrast. RSTA cannot render astral-plane characters or reorder RTL text, which
 * is why the text world moved off it.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.util.widgets.SimbrainTextPane
import org.simbrain.world.textworld.DocumentStructureDisplay
import org.simbrain.world.textworld.gui.applyDocumentStructureDisplay
import java.awt.Component
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class TextWorldUnicodeSnapshot : UiSnapshotDef {
    override val name = "text_world_unicode"

    private val sample = listOf(
        "Emoji: 😀 🚀 🧠 👍🏽 👨‍👩‍👧 🇺🇸 ❤️",
        "CJK: 日本語 中文 한국어",
        "RTL: العربية עברית",
        "Misc: café naïve é ∑ → �",
    ).joinToString("\n")

    private val chatSample = "<|im_start|>system\nYou are a helpful assistant.\n" +
        "List of tools: [{\"name\": \"current_time\"}]<|im_end|>" +
        "<|im_start|>user\nSay hi with an emoji 🎉<|im_end|>" +
        "<|im_start|>assistant\n<|tool_call_start|>[current_time()]<|tool_call_end|>" +
        "Hello! 😀 你好 مرحبا<|im_end|>"

    override fun build(): Component {
        val panel = JPanel(GridLayout(0, 1, 0, 8))
        panel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        fun addPane(title: String, component: Component) {
            panel.add(JPanel(GridLayout(0, 1)).apply {
                add(JLabel(title))
                add(component)
            })
        }
        addPane("SimbrainTextPane (text world)", SimbrainTextPane().apply { text = sample })
        addPane("SimbrainTextPane, role colors", SimbrainTextPane().apply {
            text = chatSample
            applyDocumentStructureDisplay(DocumentStructureDisplay.ROLE_COLORS)
        })
        addPane("SimbrainTextArea (RSyntaxTextArea, retired here)", SimbrainTextArea().apply { text = sample })
        addPane("JTextArea control", JTextArea(sample))
        return panel
    }
}
