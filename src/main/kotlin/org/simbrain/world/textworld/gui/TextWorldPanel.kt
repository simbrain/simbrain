/**
 * The text world's document view: a [SimbrainTextPane] (plain Swing styled text, so emoji, RTL
 * scripts and combining marks render — RSyntaxTextArea could not draw them) kept in two-way
 * sync with [TextWorld.text], plus token/span highlighting, role/structure coloring via
 * [applyDocumentStructureDisplay], and the status bar. Programmatic syncs replace the document
 * wholesale and bypass undo; user edits flow back through setTextNoEvent and trigger a
 * coalesced restyle.
 */
package org.simbrain.world.textworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.Theme
import org.simbrain.util.TokenizerResult
import org.simbrain.util.widgets.SimbrainTextPane
import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.extractEmbeddingFromCurrentText
import org.simbrain.world.textworld.textWorldPrefs
import org.simbrain.world.textworld.viewTokenEmbedding
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import javax.swing.text.View


/**
 * Display panel for reading data from user and showing text world's state.
 *
 * @author jyoshimi
 */
class TextWorldPanel(
    val world: TextWorld,
) : JPanel() {

    /**
     * Text area for inputting text into networks.
     */
    val textArea = SimbrainTextPane()

    /**
     * The main scroll panel.
     */
    val inputScrollPane: JScrollPane

    private val statusLabel = JLabel(" ").apply {
        font = Theme.label
        foreground = Theme.mutedText
    }

    private val tokenCountLabel = JLabel().apply {
        font = Theme.label
        foreground = Theme.mutedText
    }

    private var runLocked = false

    private var updatingTextArea = false

    private var restylePending = false

    private fun updateStatus() {
        val provided = world.statusMessageProvider?.invoke()
        val message = provided ?: if (runLocked) "Read-only while running" else null
        statusLabel.text = message ?: " "
        statusLabel.toolTipText = if (runLocked) RUN_LOCK_EXPLANATION else provided
        textArea.toolTipText = if (runLocked) RUN_LOCK_EXPLANATION else null
    }

    /** Locks the text while the workspace runs; the status bar says why the caret is dead. */
    fun setRunLock(locked: Boolean) {
        runLocked = locked
        textArea.isEditable = !locked
        updateStatus()
    }

    /** Shows the token count even when token boundaries are hidden. */
    private fun updateTokenCount() {
        val count = world.tokens.size
        tokenCountLabel.text = world.tokenCountLabelProvider?.invoke()
            ?: if (count == 1) "1 token" else "$count tokens"
        tokenCountLabel.toolTipText = "Tokens in this document, counted by the active tokenizer"
    }

    /**
     * Initialize the panel with an open / close toolbar.
     *
     * @param theWorld the reader world to display
     */
    init {

        this.layout = BorderLayout()
        textArea.replaceTextWithoutUndo(world.text)
        restyleDocument()
        textArea.margin = Insets(6, 8, 8, 8)
        inputScrollPane =
            JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        add(JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 10, 8, 10)
            add(inputScrollPane)
        })

        // Top toolbar
        val topToolBar = JToolBar()
        topToolBar.add(world.extractEmbeddingFromCurrentText)
        topToolBar.add(world.viewTokenEmbedding)
        topToolBar.addSeparator()
        topToolBar.add(world.textWorldPrefs)
        add(topToolBar,  BorderLayout.NORTH)

        // Status bar
        // The message rides CENTER so it ellipsizes on narrow windows (the tooltip carries the
        // full text) instead of painting through the token count in EAST.
        val statusBar = JPanel(BorderLayout(12, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.divider),
                BorderFactory.createEmptyBorder(5, 10, 12, 10)
            )
            add(statusLabel, BorderLayout.CENTER)
            add(tokenCountLabel, BorderLayout.EAST)
        }
        add(statusBar, BorderLayout.SOUTH)
        updateTokenCount()
        updateStatus()

        // Reset text position when user clicks in text area
        textArea.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                world.setPosition(textArea.caretPosition, false)
            }
        })

        // Listener for changes in the textarea (i.e. adding or removing text
        // directly in the area).
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(arg0: DocumentEvent) {
                // Attribute-only changes: all styling sweeps run through restyleDocument.
            }

            override fun insertUpdate(arg0: DocumentEvent) {
                if (updatingTextArea) return
                world.setTextNoEvent(textArea.text)
                // Clamp caret position to valid range to avoid race condition
                val validPosition = textArea.caretPosition.coerceIn(0, world.text.length)
                world.setPosition(validPosition, false)
                updateHighlights()
                updateTokenCount()
                updateStatus()
                scheduleRestyle()
            }

            override fun removeUpdate(arg0: DocumentEvent) {
                if (updatingTextArea) return
                world.setTextNoEvent(textArea.text)
                // Clamp caret position to valid range to avoid race condition
                val validPosition = textArea.caretPosition.coerceIn(0, world.text.length)
                world.setPosition(validPosition, false)
                updateHighlights()
                updateTokenCount()
                updateStatus()
                scheduleRestyle()
            }
        })

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                inputScrollPane.preferredSize =
                    Dimension(
                        this@TextWorldPanel.preferredSize.width - 25,
                        this@TextWorldPanel.preferredSize.height - 25
                    )
            }
        })
        world.events.textChanged.on(Dispatchers.Swing.immediate) {
            if (textArea.text != world.text) {
                updatingTextArea = true
                try {
                    textArea.replaceTextWithoutUndo(world.text)
                } finally {
                    updatingTextArea = false
                }
            }
            textArea.caretPosition = world.position.coerceIn(0, textArea.document.length)
            restyleDocument()
            updateHighlights()
            updateTokenCount()
            updateStatus()
        }

        world.events.cursorPositionChanged.on(Dispatchers.Swing) {
            textArea.caretPosition = world.position.coerceIn(0, textArea.document.length)
        }

        world.events.currentTokenChanged.on(Dispatchers.Swing) {
            updateHighlights()
        }

        world.events.highlightSpanChanged.on(Dispatchers.Swing) {
            updateHighlights()
            updateStatus()
        }

        world.events.preferencesChanged.on(Dispatchers.Swing) {
            updateHighlights()
            updateTokenCount()
        }

        world.events.statusChanged.on(Dispatchers.Swing) {
            updateStatus()
            updateTokenCount()
        }

    }

    /**
     * Repaints the role/structure coloring over the whole document. Styling fires document
     * change events and must stay out of both the world-sync listener and the undo history,
     * so it runs guarded and with undo suspended. Also invoked on a live theme switch by
     * [org.simbrain.workspace.gui.SimbrainDesktop], since the palette is theme-dependent.
     */
    fun restyleDocument() {
        updatingTextArea = true
        try {
            textArea.withUndoSuspended {
                textArea.applyDocumentStructureDisplay(world.documentStructureDisplay)
            }
        } finally {
            updatingTextArea = false
        }
    }

    /**
     * Restyles after the current document mutation completes: attributes cannot be changed
     * from inside a document listener, and rapid edits coalesce into one sweep.
     */
    private fun scheduleRestyle() {
        if (restylePending) return
        restylePending = true
        SwingUtilities.invokeLater {
            restylePending = false
            restyleDocument()
        }
    }

    fun updateHighlights() {
        textArea.highlighter.removeAllHighlights()
        
        // Validate that tokens match current text length to avoid race conditions
        val docLength = textArea.document.length
        if (world.tokens.isNotEmpty() && world.tokens.any { it.end > docLength }) {
            // Tokens are stale (probably waiting for events), skip highlighting until they're regenerated
            return
        }
        
        if (world.showTokenBoundaries) {
            world.tokens.forEach(::highlightToken)
        }
        world.highlightSpan?.let { span ->
            val start = span[0].coerceIn(0, docLength)
            val end = span[1].coerceIn(start, docLength)
            if (end > start) {
                highlight(start, end)
                try {
                    textArea.modelToView(start)?.let { rect ->
                        textArea.scrollRectToVisible(rect)
                    }
                } catch (e: BadLocationException) {
                    // Span temporarily out of sync with the text area, ignore
                }
            }
            return
        }
        world.tokens.getOrNull(world.currentTokenIndex)?.let { token ->
            // Double-check token is within document bounds
            if (token.end <= docLength) {
                highlight(token.start, token.end + 1)
                // Scroll to make the highlighted token visible
                try {
                    textArea.modelToView(token.start)?.let { rect ->
                        textArea.scrollRectToVisible(rect)
                    }
                } catch (e: BadLocationException) {
                    // Token position temporarily out of sync with text area, ignore
                }
            }
        }
    }

    /**
     * Draw boxes around tokens.
     */
    class TokenHighlighter : DefaultHighlightPainter(Color(0, 0, 0, 0)) {
        override fun paintLayer(
            g: Graphics, offs0: Int, offs1: Int,
            bounds: Shape?, c: JTextComponent?, view: View?
        ): Shape {
            val s = super.paintLayer(g, offs0, offs1, bounds, c, view)
            if (s is Rectangle) {
                val g2 = g.create() as Graphics2D
                g2.paint = Color.GRAY
                val r = s.bounds
                g2.drawRect(r.x, r.y, r.width, r.height - 1)
                g2.dispose()
            }
            return s
        }
    }

    internal inner class MyHighlightPainter(color: Color?) : DefaultHighlightPainter(color)

    companion object {
        private const val RUN_LOCK_EXPLANATION =
            "The workspace is running, so this document is read-only. " +
                "Pause the workspace to edit — the edit is applied on the next Play or Step."
    }

    fun highlight(begin: Int, end: Int) {
        if (!world.highlightCurrentToken) return
        // An instance of the private subclass of the default highlight painter
        val myHighlightPainter: Highlighter.HighlightPainter = MyHighlightPainter(world.highlightColor)
        removeHighlights(textArea)
        try {
            val hilite = textArea.highlighter
            hilite.addHighlight(begin, end, myHighlightPainter)
        } catch (e: BadLocationException) {
            System.err.checkError()
        }
    }

    fun highlightToken(token: TokenizerResult) {
        val tokenHighlighter = TokenHighlighter()
        try {
            textArea.highlighter.addHighlight(token.start, token.end + 1, tokenHighlighter)
        } catch (e: BadLocationException) {
            System.err.checkError()
        }
    }

    fun removeHighlights(textComp: JTextComponent) {
        val hilite = textComp.highlighter
        val hilites = hilite.highlights
        for (i in hilites.indices) {
            if (hilites[i].painter is MyHighlightPainter) {
                hilite.removeHighlight(hilites[i])
            }
        }
    }


}
