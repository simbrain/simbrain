package org.simbrain.util.widgets

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.createAction
import javax.swing.JFrame

class SimbrainTextArea : RSyntaxTextArea() {

    var lastSearchedString: String? = null
    var lastReplacedString: String? = null

    init {
        currentLineHighlightColor = background
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
        frame.title = "Find / Replace"
        frame.isVisible = true
        frame.pack()
        frame.setLocationRelativeTo(null)
    }

}