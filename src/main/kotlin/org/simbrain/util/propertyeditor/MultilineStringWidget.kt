/** Scrollable string editing with explicit bulk replacement, preserving other targets until opted in. */
package org.simbrain.util.propertyeditor

import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.reflect.KProperty

class MultilineStringWidget<O : EditableObject>(
    private val editor: AnnotatedPropertyEditor<O>,
    parameter: GuiEditable<O, String?>,
    isConsistent: Boolean
) : ParameterWidget<O, String?>(parameter, isConsistent) {
    private val bulk = editor.editingObjects.size > 1
    private var changed = false
    private var enabledByParameter = true
    val replaceText = JCheckBox("Replace text on all selected objects", !bulk)
    val textArea = JTextArea(
        if (isConsistent) parameter.value.orEmpty() else "",
        parameter.textAreaRows.coerceAtLeast(1), parameter.textAreaColumns.coerceAtLeast(1)
    ).apply {
        lineWrap = true
        wrapStyleWord = true
        isEnabled = !bulk
    }

    override val widget = JPanel(BorderLayout(0, 5)).apply {
        if (bulk) add(replaceText, BorderLayout.NORTH)
        add(JScrollPane(textArea), BorderLayout.CENTER)
        if (!isConsistent) add(JLabel("Different values; enable replacement to change them all."), BorderLayout.SOUTH)
    }

    init {
        replaceText.addActionListener {
            textArea.isEnabled = enabledByParameter && replaceText.isSelected
            if (replaceText.isSelected) this@MultilineStringWidget.isConsistent = true
            events.valueChanged.fire(parameter.property)
        }
        textArea.document.addDocumentListener(object : DocumentListener {
            private fun edited() {
                changed = true
                this@MultilineStringWidget.isConsistent = true
                events.valueChanged.fire(parameter.property)
            }
            override fun insertUpdate(e: DocumentEvent) = edited()
            override fun removeUpdate(e: DocumentEvent) = edited()
            override fun changedUpdate(e: DocumentEvent) = edited()
        })
    }

    override val value: String?
        get() = if (changed || bulk && replaceText.isSelected) textArea.text else parameter.value

    override fun valueFor(editingObject: O): String? =
        if (bulk && !replaceText.isSelected) parameter.property.get(editingObject) else value

    override fun refresh(property: KProperty<*>) {
        parameter.update(UpdateFunctionContext(editor, parameter, property,
            enableWidgetProvider = {
                enabledByParameter = it
                replaceText.isEnabled = it
                textArea.isEnabled = it && replaceText.isSelected
            },
            widgetVisibilityProvider = { widget.isVisible = it }
        ))
    }
}
