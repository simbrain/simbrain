/**
 * A plain-Swing styled text pane for natural-language documents, used where
 * [org.fife.ui.rsyntaxtextarea.RSyntaxTextArea]'s custom renderer is unsuitable: RSTA cannot
 * render characters outside the basic multilingual plane (emoji become placeholder boxes),
 * reorder right-to-left scripts, or compose combining marks, all of which language models emit.
 * Provides the editing chrome RSTA supplied out of the box — undo/redo with keyboard bindings,
 * a cut/copy/paste context menu, and a find/replace dialog ([PlainFindReplaceDialog]) — plus
 * wrap-anywhere line breaking so long unbroken model output never clips under a
 * never-horizontal scroll pane. Styling sweeps (e.g. role coloring) should run inside
 * [withUndoSuspended] so they never enter the undo history.
 */
package org.simbrain.util.widgets

import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.KeyCombination
import org.simbrain.util.Shift
import org.simbrain.util.put
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTextPane
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.AbstractDocument
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Element
import javax.swing.text.LabelView
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException
import javax.swing.undo.UndoManager

class SimbrainTextPane : JTextPane() {

    var lastSearchedString: String? = null
    var lastReplacedString: String? = null

    private val undoManager = UndoManager()

    private var undoSuspended = false

    init {
        editorKit = WrapAnywhereEditorKit()
        // The LaF-installed highlighter is a UIResource, which BasicTextUI replaces on every
        // updateUI (e.g. a live theme switch), silently dropping all highlights. A plain
        // DefaultHighlighter is left alone, so highlights survive.
        highlighter = DefaultHighlighter()
        document.addUndoableEditListener { e ->
            if (!undoSuspended) undoManager.addEdit(e.edit)
        }
        bindWhenFocused(CmdOrCtrl + 'Z', "simbrain-undo") {
            if (isEditable) try {
                undoManager.undo()
            } catch (e: CannotUndoException) {
            }
        }
        val redo = {
            if (isEditable) try {
                undoManager.redo()
            } catch (e: CannotRedoException) {
            }
        }
        bindWhenFocused(CmdOrCtrl + Shift + 'Z', "simbrain-redo", redo)
        bindWhenFocused(CmdOrCtrl + 'Y', "simbrain-redo-y", redo)
        bindWhenFocused(CmdOrCtrl + 'F', "simbrain-find") { showFindReplaceDialog() }
        componentPopupMenu = buildPopupMenu()
    }

    /**
     * Reads and writes the document text directly. JEditorPane's implementations round-trip
     * through the editor kit, which rewrites line endings; document text must stay exactly
     * what couplings and the text world model see.
     */
    override fun getText(): String = try {
        document.getText(0, document.length)
    } catch (e: BadLocationException) {
        ""
    }

    override fun setText(t: String?) {
        try {
            val doc = document
            if (doc is AbstractDocument) {
                doc.replace(0, doc.length, t ?: "", null)
            } else {
                doc.remove(0, doc.length)
                doc.insertString(0, t ?: "", null)
            }
        } catch (e: BadLocationException) {
            throw IllegalArgumentException(e.message)
        }
    }

    /**
     * Runs [block] with undo recording off. Use for attribute sweeps, which never invalidate
     * document offsets, so suspending without clearing history is safe.
     */
    fun withUndoSuspended(block: () -> Unit) {
        undoSuspended = true
        try {
            block()
        } finally {
            undoSuspended = false
        }
    }

    /**
     * Replaces the whole document without recording undo, then clears the undo history:
     * unrecorded content changes invalidate the offsets earlier edits refer to. Use for
     * programmatic syncs (e.g. a model streaming into the document) so the user's undo only
     * ever covers their own edits against the current content.
     */
    fun replaceTextWithoutUndo(t: String) {
        withUndoSuspended { text = t }
        undoManager.discardAllEdits()
    }

    fun showFindReplaceDialog() {
        val frame = JFrame()
        val dialog = PlainFindReplaceDialog(frame, this)
        frame.contentPane = dialog
        frame.title = if (isEditable) "Find / Replace..." else "Find..."
        frame.isVisible = true
        frame.pack()
        frame.setLocationRelativeTo(null)
    }

    private fun bindWhenFocused(combo: KeyCombination, name: String, action: () -> Unit) {
        combo.withKeyStroke { getInputMap(JComponent.WHEN_FOCUSED).put(it, name) }
        actionMap.put(name) { action() }
    }

    private fun buildPopupMenu() = JPopupMenu().apply {
        val undoItem = JMenuItem("Undo").apply { addActionListener { if (undoManager.canUndo()) undoManager.undo() } }
        val redoItem = JMenuItem("Redo").apply { addActionListener { if (undoManager.canRedo()) undoManager.redo() } }
        val cutItem = JMenuItem("Cut").apply { addActionListener { cut() } }
        val copyItem = JMenuItem("Copy").apply { addActionListener { copy() } }
        val pasteItem = JMenuItem("Paste").apply { addActionListener { paste() } }
        val selectAllItem = JMenuItem("Select All").apply { addActionListener { selectAll() } }
        add(undoItem)
        add(redoItem)
        addSeparator()
        add(cutItem)
        add(copyItem)
        add(pasteItem)
        addSeparator()
        add(selectAllItem)
        addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                undoItem.isEnabled = isEditable && undoManager.canUndo()
                redoItem.isEnabled = isEditable && undoManager.canRedo()
                cutItem.isEnabled = isEditable && selectedText != null
                copyItem.isEnabled = selectedText != null
                pasteItem.isEnabled = isEditable
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    /**
     * Standard word wrap breaks only at whitespace, so a long unbroken run (a URL, a stream of
     * CJK text) forces the pane wider than its viewport and gets clipped when horizontal
     * scrolling is disabled. Letting label views shrink to zero minimum width allows a break
     * anywhere when no word boundary fits, matching JTextArea's lineWrap behavior.
     */
    private class WrapAnywhereEditorKit : StyledEditorKit() {
        private val factory = ViewFactory { elem ->
            val view = defaultFactory.create(elem)
            if (view is LabelView && elem.name == AbstractDocument.ContentElementName) {
                WrapAnywhereLabelView(elem)
            } else {
                view
            }
        }

        private val defaultFactory = super.getViewFactory()

        override fun getViewFactory(): ViewFactory = factory
    }

    private class WrapAnywhereLabelView(elem: Element) : LabelView(elem) {
        override fun getMinimumSpan(axis: Int): Float =
            if (axis == View.X_AXIS) 0f else super.getMinimumSpan(axis)
    }
}
