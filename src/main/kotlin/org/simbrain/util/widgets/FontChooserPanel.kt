/** List-based font editing with a draft preview and independent changes for mixed selections. */
package org.simbrain.util.widgets

import net.miginfocom.swing.MigLayout
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class FontChanges(val family: String? = null, val style: Int? = null, val size: Int? = null) {
    fun applyTo(font: Font): Font = Font(family ?: font.name, style ?: font.style, size ?: font.size)
    fun then(next: FontChanges) = FontChanges(next.family ?: family, next.style ?: style, next.size ?: size)
}

class FontChooserPanel(private val fonts: List<Font>) : JPanel(
    MigLayout("insets 10, wrap 3, fillx", "[grow 50,fill][grow 30,fill][grow 20,fill]")
) {
    private val families = (listOf(Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED, Font.DIALOG) +
        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames + fonts.map { it.name })
        .distinct().sorted().toTypedArray()
    val familyList = JList(families)
    val styleList = JList(arrayOf("Regular", "Bold", "Italic", "Bold Italic"))
    val sizeList = JList(arrayOf("8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"))
    val familyField = JTextField(fonts.map { it.name }.distinct().singleOrNull().orEmpty(), 20)
    val styleField = JTextField().apply { isEditable = false }
    val sizeField = JTextField(fonts.map { it.size }.distinct().singleOrNull()?.toString().orEmpty(), 5)
    val preview = JLabel("AaBbYyZz", SwingConstants.CENTER)
    private var familyChanged = false
    private var styleChanged = false
    private var sizeChanged = false

    init {
        require(fonts.isNotEmpty())
        familyList.setSelectedValue(familyField.text, true)
        fonts.map { it.style }.distinct().singleOrNull()?.let { styleList.selectedIndex = it }
        styleField.text = styleList.selectedValue.orEmpty()
        sizeList.setSelectedValue(sizeField.text, true)
        listOf(familyList, styleList, sizeList).forEach {
            it.selectionMode = ListSelectionModel.SINGLE_SELECTION
            it.visibleRowCount = 8
        }
        add(JLabel("Font:")); add(JLabel("Font style:")); add(JLabel("Size:"))
        add(familyField); add(styleField); add(sizeField)
        add(JScrollPane(familyList), "grow")
        add(JScrollPane(styleList), "grow")
        add(JScrollPane(sizeList), "grow")
        add(JPanel(java.awt.BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Sample")
            add(preview)
        }, "span 3, growx, h 100!")
        if (fonts.distinct().size > 1) add(JLabel("Blank fields have mixed values. Unchanged attributes are preserved."), "span 3")
        familyList.addListSelectionListener {
            if (!it.valueIsAdjusting) familyList.selectedValue?.let { value -> familyField.text = value }
        }
        styleList.addListSelectionListener {
            if (!it.valueIsAdjusting && styleList.selectedIndex >= 0) {
                styleChanged = true
                styleField.text = styleList.selectedValue
                updatePreview()
            }
        }
        sizeList.addListSelectionListener {
            if (!it.valueIsAdjusting) sizeList.selectedValue?.let { value -> sizeField.text = value }
        }
        familyField.onEdit { familyChanged = true; updatePreview() }
        sizeField.onEdit { sizeChanged = true; updatePreview() }
        updatePreview()
    }

    val hasValidInput: Boolean
        get() = (!familyChanged || familyField.text.isNotBlank()) &&
            (!sizeChanged || (sizeField.text.toIntOrNull() ?: 0) > 0)

    override fun addNotify() {
        super.addNotify()
        SwingUtilities.invokeLater {
            listOf(familyList, styleList, sizeList).forEach {
                if (it.selectedIndex >= 0) it.ensureIndexIsVisible(it.selectedIndex)
            }
        }
    }

    fun changes(): FontChanges {
        require(hasValidInput)
        return FontChanges(familyField.text.takeIf { familyChanged },
            styleList.selectedIndex.takeIf { styleChanged },
            sizeField.text.toIntOrNull().takeIf { sizeChanged })
    }

    private fun updatePreview() {
        if (hasValidInput) preview.font = changes().applyTo(fonts.first())
    }

    private fun JTextField.onEdit(block: () -> Unit) {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = block()
            override fun removeUpdate(e: DocumentEvent) = block()
            override fun changedUpdate(e: DocumentEvent) = block()
        })
    }
}
