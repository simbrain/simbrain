package org.simbrain.util.widgets

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Theme
import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.createAction
import javax.swing.JFrame
import javax.swing.UIManager

/**
 * [RSyntaxTextArea] does its own theming (via bundled XML themes) and does not follow FlatLaf, so it
 * stays light in dark mode unless it is explicitly re-themed. Loads the matching bundled RSyntax theme
 * for the active Look-and-Feel, preserving the current font. Called at construction and re-invoked on a
 * live theme switch by [org.simbrain.workspace.gui.SimbrainDesktop.refreshThemedChrome].
 */
fun RSyntaxTextArea.applyLafSyntaxTheme() {
    val themePath = if (UIManager.getBoolean("laf.dark")) {
        "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
    } else {
        "/org/fife/ui/rsyntaxtextarea/themes/default.xml"
    }
    try {
        Theme::class.java.getResourceAsStream(themePath)?.use { Theme.load(it, font).apply(this) }
    } catch (e: Exception) {
        // Keep RSyntaxTextArea defaults if the bundled theme cannot be loaded.
    }
    currentLineHighlightColor = background
}

class SimbrainTextArea : RSyntaxTextArea() {

    var lastSearchedString: String? = null
    var lastReplacedString: String? = null

    init {
        applyLafSyntaxTheme()
        createAction(
            name = "Find / Replace...",
            description = "Find and replace text...",
            keyboardShortcut = CmdOrCtrl + 'F'
        ) {
            showFindReplaceDialog()
        }
    }

    fun showFindReplaceDialog() {
        val frame = JFrame()
        val dialog = FindReplaceDialog(frame, this)
        frame.contentPane = dialog
        frame.title = if (isEditable) "Find / Replace..." else "Find..."
        frame.isVisible = true
        frame.pack()
        frame.setLocationRelativeTo(null)
    }

}