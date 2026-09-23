/** Presents a Font object compactly and accepts drafts from its dedicated font chooser. */
package org.simbrain.util.propertyeditor

import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.showWarningDialog
import org.simbrain.util.widgets.FontChanges
import org.simbrain.util.widgets.FontChooserPanel
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*
import kotlin.reflect.KProperty

class FontWidget<O : EditableObject>(
    private val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, Font>,
    isConsistent: Boolean
) : ParameterWidget<O, Font>(parameter, isConsistent) {
    private var pending = FontChanges()
    private val summary = JLabel()
    val editButton = JButton("Edit...")

    override val widget = JPanel(BorderLayout(10, 0)).apply {
        add(summary, BorderLayout.CENTER)
        add(editButton, BorderLayout.EAST)
    }

    init {
        updateSummary()
        editButton.addActionListener {
            val chooser = createEditor()
            object : StandardDialog(SwingUtilities.getWindowAncestor(widget), "Font") {
                override fun closeDialogOk() {
                    if (!chooser.hasValidInput) {
                        showWarningDialog("Enter a font name and a positive whole-number size.", "Invalid font")
                        return
                    }
                    super.closeDialogOk()
                }
            }.apply {
                contentPane = chooser
                isModal = true
                addCommitTask { acceptEdits(chooser) }
                pack()
            }.display()
        }
    }

    fun createEditor() = FontChooserPanel(editor.editingObjects.map { valueFor(it) })

    fun acceptEdits(chooser: FontChooserPanel) {
        pending = pending.then(chooser.changes())
        isConsistent = true
        updateSummary()
        events.valueChanged.fire(parameter.property)
    }

    private fun updateSummary() {
        val fonts = editor.editingObjects.map { valueFor(it) }
        summary.text = fonts.distinct().singleOrNull()?.let {
            val style = arrayOf("Regular", "Bold", "Italic", "Bold Italic")[it.style]
            "${it.name}, $style, ${it.size}"
        } ?: "Mixed fonts"
    }

    override val value: Font get() = pending.applyTo(parameter.value)
    override fun valueFor(editingObject: O): Font = pending.applyTo(parameter.property.get(editingObject))

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(editor, parameter, property,
            enableWidgetProvider = { editButton.isEnabled = it },
            widgetVisibilityProvider = { widget.isVisible = it }
        ))
    }
}
