package org.simbrain.docviewer

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.geom.Point2D
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.HyperlinkEvent
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter

/**
 * Renders documentation as html and provides a browser style inline find bar (Cmd/Ctrl+F) over the rendered text,
 * with next / previous navigation, a live match count, case sensitivity, scroll-to-match, and a remembered search term.
 */
class DocViewerViewPanel : JPanel(BorderLayout()) {

    var text: String
        get() = renderedTextPanel.text
        set(value) {
            renderedTextPanel.text = value
            // The document changed so any prior match offsets are stale.
            if (findBar.parent != null && findField.text.isNotEmpty()) {
                SwingUtilities.invokeLater { runSearch(Anchor.VIEWPORT) }
            } else {
                renderedTextPanel.highlighter.removeAllHighlights()
            }
        }

    val renderedTextPanel = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    e.url?.let { Desktop.getDesktop().browse(processLocalFiles(it.toURI())) }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                } catch (ex: URISyntaxException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private val scrollPane = JScrollPane(renderedTextPanel)

    private val allMatchPainter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 230, 90))
    private val currentMatchPainter = DefaultHighlighter.DefaultHighlightPainter(Color(255, 150, 50))

    private var matches: List<IntRange> = emptyList()
    private var currentMatchIndex = -1
    private var suppressSearch = false

    private val findField = JTextField(16)
    private val prevButton = JButton("‹").apply { toolTipText = "Previous match (Shift+Enter)" }
    private val nextButton = JButton("›").apply { toolTipText = "Next match (Enter)" }
    private val countLabel = JLabel()
    private val matchCaseToggle = JToggleButton("Aa").apply { toolTipText = "Match case" }
    private val closeButton = JButton("✕").apply { toolTipText = "Close (Esc)" }

    private val findBar = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        add(JLabel("Find: "))
        add(findField)
        add(Box.createHorizontalStrut(4))
        add(prevButton)
        add(nextButton)
        add(Box.createHorizontalStrut(8))
        add(countLabel)
        add(Box.createHorizontalStrut(8))
        add(matchCaseToggle)
        add(Box.createHorizontalStrut(4))
        add(closeButton)
    }

    init {
        add(scrollPane, BorderLayout.CENTER)

        val fieldHeight = findField.preferredSize.height
        findField.minimumSize = Dimension(40, fieldHeight)
        findField.maximumSize = Dimension(Int.MAX_VALUE, fieldHeight)
        countLabel.preferredSize = Dimension(64, fieldHeight)
        countLabel.minimumSize = Dimension(0, fieldHeight)
        countLabel.maximumSize = Dimension(64, fieldHeight)

        findField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = onFieldChanged()
            override fun removeUpdate(e: DocumentEvent?) = onFieldChanged()
            override fun changedUpdate(e: DocumentEvent?) = onFieldChanged()
        })

        prevButton.addActionListener { selectPrevious() }
        nextButton.addActionListener { selectNext() }
        closeButton.addActionListener { closeFindBar() }
        matchCaseToggle.addActionListener {
            lastMatchCase = matchCaseToggle.isSelected
            runSearch(Anchor.KEEP_CURRENT)
            findField.requestFocusInWindow()
        }

        val menuMask = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        bindKey(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyEvent.VK_F, menuMask) { openFindBar() }
        findField.bindKey(JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER, 0) { selectNext() }
        findField.bindKey(JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK) { selectPrevious() }
        findField.bindKey(JComponent.WHEN_FOCUSED, KeyEvent.VK_ESCAPE, 0) { closeFindBar() }

        // Enter / Shift+Enter / Esc also work when a bar button has focus.
        findBar.bindKey(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyEvent.VK_ENTER, 0) { selectNext() }
        findBar.bindKey(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK) { selectPrevious() }
        findBar.bindKey(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyEvent.VK_ESCAPE, 0) { closeFindBar() }
    }

    private fun onFieldChanged() {
        if (suppressSearch) return
        lastSearch = findField.text
        runSearch(Anchor.KEEP_CURRENT)
    }

    private fun openFindBar() {
        if (findBar.parent == null) {
            add(findBar, BorderLayout.NORTH)
            revalidate()
            repaint()
        }
        suppressSearch = true
        findField.text = lastSearch
        matchCaseToggle.isSelected = lastMatchCase
        suppressSearch = false
        runSearch(Anchor.VIEWPORT)
        SwingUtilities.invokeLater {
            findField.requestFocusInWindow()
            findField.selectAll()
        }
    }

    private fun closeFindBar() {
        if (findBar.parent != null) {
            remove(findBar)
            revalidate()
            repaint()
        }
        renderedTextPanel.highlighter.removeAllHighlights()
        matches = emptyList()
        currentMatchIndex = -1
        renderedTextPanel.requestFocusInWindow()
    }

    private enum class Anchor { VIEWPORT, KEEP_CURRENT }

    private fun runSearch(anchor: Anchor) {
        val previousStart = matches.getOrNull(currentMatchIndex)?.first
        val term = findField.text
        matches = computeMatches(term)
        currentMatchIndex = if (matches.isEmpty()) {
            -1
        } else {
            val from = when (anchor) {
                Anchor.VIEWPORT -> firstVisibleOffset()
                Anchor.KEEP_CURRENT -> previousStart ?: firstVisibleOffset()
            }
            matches.indexOfFirst { it.first >= from }.let { if (it < 0) 0 else it }
        }
        applyHighlights()
        if (currentMatchIndex >= 0) scrollToCurrent()
        updateStatus(term)
    }

    private fun computeMatches(term: String): List<IntRange> {
        if (term.isEmpty()) return emptyList()
        val content = renderedTextPanel.document.let { it.getText(0, it.length) }
        val ignoreCase = !matchCaseToggle.isSelected
        val result = ArrayList<IntRange>()
        var i = content.indexOf(term, 0, ignoreCase)
        while (i >= 0) {
            result.add(i until i + term.length)
            i = content.indexOf(term, i + term.length, ignoreCase)
        }
        return result
    }

    private fun applyHighlights() {
        val highlighter = renderedTextPanel.highlighter
        highlighter.removeAllHighlights()
        matches.forEachIndexed { index, range ->
            val painter = if (index == currentMatchIndex) currentMatchPainter else allMatchPainter
            try {
                highlighter.addHighlight(range.first, range.last + 1, painter)
            } catch (e: BadLocationException) {
                e.printStackTrace()
            }
        }
    }

    private fun selectNext() {
        if (matches.isEmpty()) return
        currentMatchIndex = (currentMatchIndex + 1) % matches.size
        applyHighlights()
        scrollToCurrent()
        updateStatus(findField.text)
    }

    private fun selectPrevious() {
        if (matches.isEmpty()) return
        currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
        applyHighlights()
        scrollToCurrent()
        updateStatus(findField.text)
    }

    private fun scrollToCurrent() {
        val range = matches.getOrNull(currentMatchIndex) ?: return
        SwingUtilities.invokeLater {
            try {
                val rect = renderedTextPanel.modelToView2D(range.first) ?: return@invokeLater
                val bounds = rect.bounds
                renderedTextPanel.scrollRectToVisible(
                    Rectangle(bounds.x, bounds.y - 24, bounds.width, bounds.height + 48)
                )
            } catch (e: BadLocationException) {
                e.printStackTrace()
            }
        }
    }

    private fun firstVisibleOffset(): Int {
        val viewRect = scrollPane.viewport.viewRect
        return renderedTextPanel.viewToModel2D(Point2D.Double(viewRect.x.toDouble(), viewRect.y.toDouble()))
            .coerceAtLeast(0)
    }

    private fun updateStatus(term: String) {
        countLabel.text = when {
            term.isEmpty() -> ""
            matches.isEmpty() -> "No results"
            else -> "${currentMatchIndex + 1} / ${matches.size}"
        }
        val hasMatches = matches.isNotEmpty()
        prevButton.isEnabled = hasMatches
        nextButton.isEnabled = hasMatches
    }

    private fun processLocalFiles(uri: URI): URI {
        val uriStr = uri.toString()
        return if (uriStr.startsWith("//localfiles/")) {
            try {
                Paths.get(System.getProperty("user.dir"), uriStr.substring(5)).toUri()
            } catch (e: Exception) {
                e.printStackTrace()
                uri
            }
        } else {
            uri
        }
    }

    private fun action(block: () -> Unit) = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) = block()
    }

    private fun JComponent.bindKey(condition: Int, keyCode: Int, modifiers: Int, block: () -> Unit) {
        val name = "key_${condition}_${keyCode}_$modifiers"
        getInputMap(condition).put(KeyStroke.getKeyStroke(keyCode, modifiers), name)
        actionMap.put(name, action(block))
    }

    companion object {
        private var lastSearch = ""
        private var lastMatchCase = false
    }
}
