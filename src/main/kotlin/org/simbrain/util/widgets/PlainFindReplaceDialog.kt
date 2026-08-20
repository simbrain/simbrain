/**
 * Find / replace panel for [SimbrainTextPane], mirroring the RSyntaxTextArea-based
 * [FindReplaceDialog] but implemented with plain regex search over the document text, since
 * RSTA's SearchEngine only operates on RTextArea subclasses. Hosted in a frame by
 * [SimbrainTextPane.showFindReplaceDialog]; replace controls are omitted when the pane is
 * read-only.
 */
package org.simbrain.util.widgets

import net.miginfocom.swing.MigLayout
import org.simbrain.util.showInfoDialog
import org.simbrain.util.showWarningDialog
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class PlainFindReplaceDialog(frame: JFrame, private val textPane: SimbrainTextPane) : JPanel() {

    private val searchField = JTextField(16).apply { text = textPane.lastSearchedString }
    private var replaceField: JTextField? = null
    private val regexCB = JCheckBox("Regex")
    private val matchCaseCB = JCheckBox("Match Case")
    private val wholeWordCB = JCheckBox("Whole Word")
    private val wrapSearchCB = JCheckBox("Wrap").apply { isSelected = true }
    private val backwardSearch = JRadioButton("Backward")
    private val forwardSearch = JRadioButton("Forward").apply { isSelected = true }

    init {
        val showReplace = textPane.isEditable
        layout = MigLayout(
            "insets 15, gap 8",
            "[right][grow, fill][fill][fill]",
            "[]8[]12[]8[]"
        )

        val findButton = JButton("Find Next").apply {
            addActionListener {
                textPane.lastSearchedString = searchField.text
                find()
            }
        }

        add(JLabel("Find:"))
        add(searchField, "growx")

        if (showReplace) {
            add(findButton)
            add(JButton("Replace & Find").apply {
                addActionListener {
                    textPane.lastSearchedString = searchField.text
                    textPane.lastReplacedString = replaceField?.text
                    replaceSelection()
                    find()
                }
            }, "wrap")

            val theReplaceField = JTextField(16).apply { text = textPane.lastReplacedString }
            replaceField = theReplaceField
            add(JLabel("Replace:"))
            add(theReplaceField, "growx")
            add(JButton("Replace").apply {
                addActionListener {
                    textPane.lastReplacedString = theReplaceField.text
                    replaceSelection()
                }
            })
            add(JButton("Replace All").apply {
                addActionListener {
                    textPane.lastReplacedString = theReplaceField.text
                    replaceAll()
                }
            }, "wrap")
        } else {
            add(findButton, "span 2, wrap")
        }

        add(JLabel())
        add(JPanel(MigLayout("insets 0, gap 15", "[][][][]", "[]")).apply {
            add(matchCaseCB)
            add(wholeWordCB)
            add(regexCB)
            add(wrapSearchCB)
        }, "span 3, align left, wrap")

        add(JLabel())
        add(JPanel(MigLayout("insets 0, gap 10", "[][]", "[]")).apply {
            ButtonGroup().apply {
                add(forwardSearch)
                add(backwardSearch)
            }
            add(forwardSearch)
            add(backwardSearch)
        })
        add(JLabel(), "growx")
        add(JButton("Close").apply { addActionListener { frame.dispose() } }, "align right, wrap")

        frame.rootPane.defaultButton = findButton
    }

    private fun buildPattern(): Pattern? {
        val raw = searchField.text.orEmpty()
        if (raw.isEmpty()) return null
        var patternText = if (regexCB.isSelected) raw else Pattern.quote(raw)
        if (wholeWordCB.isSelected) patternText = "\\b(?:$patternText)\\b"
        var flags = 0
        if (!matchCaseCB.isSelected) flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        return try {
            Pattern.compile(patternText, flags)
        } catch (e: PatternSyntaxException) {
            showWarningDialog("Invalid regular expression: ${e.description}")
            null
        }
    }

    private fun find() {
        val pattern = buildPattern() ?: return
        val matcher = pattern.matcher(textPane.text)
        val match = if (forwardSearch.isSelected) {
            findForwardFrom(matcher, textPane.selectionEnd)
                ?: if (wrapSearchCB.isSelected) findForwardFrom(matcher, 0) else null
        } else {
            findBackwardBefore(matcher, textPane.selectionStart)
                ?: if (wrapSearchCB.isSelected) findBackwardBefore(matcher, Int.MAX_VALUE) else null
        }
        if (match != null) {
            textPane.requestFocusInWindow()
            textPane.select(match.first, match.second)
        }
    }

    private fun findForwardFrom(matcher: Matcher, from: Int): Pair<Int, Int>? {
        matcher.reset()
        return if (matcher.find(from.coerceIn(0, textPane.document.length))) {
            matcher.start() to matcher.end()
        } else {
            null
        }
    }

    /** The last match ending at or before [limit], so repeated backward finds walk the document. */
    private fun findBackwardBefore(matcher: Matcher, limit: Int): Pair<Int, Int>? {
        matcher.reset()
        var found: Pair<Int, Int>? = null
        while (matcher.find()) {
            if (matcher.end() > limit) break
            found = matcher.start() to matcher.end()
        }
        return found
    }

    private fun replacementText(): String {
        val raw = replaceField?.text.orEmpty()
        return if (regexCB.isSelected) raw else Matcher.quoteReplacement(raw)
    }

    /** Replaces the selection when it is exactly a match, expanding regex group references. */
    private fun replaceSelection() {
        val pattern = buildPattern() ?: return
        val selected = textPane.selectedText ?: return
        val matcher = pattern.matcher(selected)
        if (matcher.matches()) {
            val replacement = try {
                matcher.replaceFirst(replacementText())
            } catch (e: RuntimeException) {
                showWarningDialog("Invalid replacement: ${e.message}")
                return
            }
            textPane.replaceSelection(replacement)
        }
    }

    private fun replaceAll() {
        val pattern = buildPattern() ?: return
        val matcher = pattern.matcher(textPane.text)
        var count = 0
        while (matcher.find()) count++
        if (count > 0) {
            matcher.reset()
            val replaced = try {
                matcher.replaceAll(replacementText())
            } catch (e: RuntimeException) {
                showWarningDialog("Invalid replacement: ${e.message}")
                return
            }
            textPane.text = replaced
        }
        showInfoDialog("$count occurrence(s) replaced.", "Replace All")
    }
}
