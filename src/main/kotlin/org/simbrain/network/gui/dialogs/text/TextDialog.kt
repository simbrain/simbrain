/** Shared APE text/font dialog for creation and editing; creation callbacks run only after accepting the draft. */
package org.simbrain.network.gui.dialogs.text

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.StandardDialog
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.MultilineStringWidget
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class TextDialog(
    models: List<NetworkTextObject>,
    titleName: String = if (models.size == 1) "Edit Text" else "Edit ${models.size} Text Objects",
    onCommit: (List<NetworkTextObject>) -> Unit = {}
) : StandardDialog() {
    val editor = TextEditorPanel(models)

    init {
        title = titleName
        contentPane = editor
        addCommitTask {
            editor.commitChanges()
            onCommit(models)
        }
        pack()
    }
}

class TextEditorPanel(models: List<NetworkTextObject>) : JPanel(BorderLayout()) {
    val propertyEditor = AnnotatedPropertyEditor(models.distinct().map { TextProperties(it) })
    private val textWidget get() = propertyEditor.propertyNameWidgetMap["text"] as MultilineStringWidget
    val replaceText get() = textWidget.replaceText
    val textArea get() = textWidget.textArea

    init {
        add(propertyEditor, BorderLayout.CENTER)
    }

    fun commitChanges() = propertyEditor.commitChanges()
}

/** Exposes text and a composite font property to APE without changing the serialized model format. */
class TextProperties(private val model: NetworkTextObject) : EditableObject {
    @UserParameter(label = "Text", multiline = true, textAreaRows = 5, textAreaColumns = 32, order = 1)
    var text: String
        get() = model.text
        set(value) { model.text = value }

    @UserParameter(label = "Font", description = "Text font family, size, and style", order = 2)
    var font: Font
        get() = Font(model.fontName, (if (model.isBold) Font.BOLD else Font.PLAIN) or
            (if (model.isItalic) Font.ITALIC else Font.PLAIN), model.fontSize)
        set(value) {
            model.fontName = value.name
            model.fontSize = value.size
            model.isBold = value.isBold
            model.isItalic = value.isItalic
            model.events.textUpdated.fire()
        }

    override val name = "Text object"
}
