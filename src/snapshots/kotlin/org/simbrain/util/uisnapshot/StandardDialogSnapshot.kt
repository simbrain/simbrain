package org.simbrain.util.uisnapshot

import org.simbrain.network.updaterules.IzhikevichRule
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Component
import javax.swing.JButton

/**
 * A plain [StandardDialog] with a subclass-supplied "Help" button, to exercise the
 * full button bar: Help on the left, OK/Cancel right-aligned.
 */
class StandardDialogSnapshot : UiSnapshotDef {
    override val name = "standard_dialog"

    override fun build(): Component {
        val editor = AnnotatedPropertyEditor(listOf(IzhikevichRule()))
        val dialog = StandardDialog()
        dialog.contentPane = editor
        dialog.title = "Standard Dialog"
        dialog.addButton(JButton("Help"))
        return dialog
    }
}
