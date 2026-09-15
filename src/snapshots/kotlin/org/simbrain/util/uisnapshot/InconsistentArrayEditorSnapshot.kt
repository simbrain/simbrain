package org.simbrain.util.uisnapshot

import org.simbrain.network.core.NeuronArray
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.TableParameterWidget
import java.awt.Component
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Two neuron arrays with different activations edited together: the left editor shows the "..." placeholder for
 * the array fields, the right one shows the same editor after choosing to edit the activations.
 */
class InconsistentArrayEditorSnapshot : UiSnapshotDef {
    override val name = "inconsistent_array_editor"

    override fun build(): Component {
        fun arrays() = listOf(NeuronArray(4), NeuronArray(4)).also { list ->
            list[1].activations.set(0, 0, 1.0)
        }
        val placeholder = AnnotatedPropertyEditor(arrays())
        val edited = AnnotatedPropertyEditor(arrays()).also {
            (it.propertyNameWidgetMap["activations"] as TableParameterWidget<*, *>).editInconsistentValues()
        }
        return JPanel(GridLayout(1, 2, 12, 0)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(placeholder)
            add(edited)
        }
    }
}
