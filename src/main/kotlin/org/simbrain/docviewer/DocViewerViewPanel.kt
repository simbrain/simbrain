package org.simbrain.docviewer

import java.awt.*
import java.awt.event.KeyEvent
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter

class DocViewerViewPanel : JScrollPane() {

    var text: String
        get() = renderedTextPanel.text
        set(value) {
            renderedTextPanel.text = value
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

        // Simple search capability
        val key = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx)
        getInputMap(JComponent.WHEN_FOCUSED).put(key, "openSearchDialog")
        actionMap.put("openSearchDialog", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                showSearchDialog()
            }
        })
    }.also {
        setViewportView(it)
    }

    private fun showSearchDialog() {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(this), "Find Text", Dialog.ModalityType.APPLICATION_MODAL)
        val searchField = JTextField(20)
        val findButton = JButton("Find")

        findButton.addActionListener {
            highlightText(searchField.text)
        }

        dialog.layout = FlowLayout()
        dialog.add(JLabel("Search:"))
        dialog.add(searchField)
        dialog.add(findButton)

        dialog.rootPane.defaultButton = findButton // Pressing Enter triggers "Find"
        dialog.pack()
        dialog.setLocationRelativeTo(this)

        // Focus search field when dialog shows
        SwingUtilities.invokeLater { searchField.requestFocusInWindow() }

        dialog.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: java.awt.event.WindowEvent?) {
                renderedTextPanel.highlighter.removeAllHighlights()
            }

            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                renderedTextPanel.highlighter.removeAllHighlights()
            }
        })

        // Escape closes the dialog
        dialog.rootPane.registerKeyboardAction(
            { dialog.dispose() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )

        dialog.isVisible = true
    }

    private fun highlightText(textToFind: String) {
        val highlighter = renderedTextPanel.highlighter
        highlighter.removeAllHighlights()

        if (textToFind.isBlank()) return

        val content = renderedTextPanel.document.getText(0, renderedTextPanel.document.length)
        val pattern = textToFind.toRegex(RegexOption.IGNORE_CASE)

        pattern.findAll(content).forEach {
            try {
                val p0 = it.range.first
                val p1 = it.range.last + 1
                highlighter.addHighlight(p0, p1, DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW))
            } catch (e: BadLocationException) {
                e.printStackTrace()
            }
        }

        // Optional: scroll to first match
        pattern.find(content)?.range?.firstOrNull()?.let {
            renderedTextPanel.caretPosition = it
        }
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
}
