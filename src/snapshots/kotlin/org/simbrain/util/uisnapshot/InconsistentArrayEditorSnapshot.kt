package org.simbrain.util.uisnapshot

import org.simbrain.network.core.NeuronArray
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Component
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Neuron arrays with different activations edited together: the left editor shows "..." in the one activation cell
 * that differs, the right one shows the "..." label that stands in for the table when the arrays have different
 * sizes.
 */
class InconsistentArrayEditorSnapshot : UiSnapshotDef {
    override val name = "inconsistent_array_editor"

    override fun build(): Component {
        val sameSize = listOf(NeuronArray(4), NeuronArray(4)).onEach { it.clear() }.also { list ->
            list[1].activations.set(1, 0, 1.0)
        }
        val differentSizes = listOf(NeuronArray(4), NeuronArray(3))
        return JPanel(GridLayout(1, 2, 12, 0)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(AnnotatedPropertyEditor(sameSize))
            add(AnnotatedPropertyEditor(differentSizes))
        }
    }
}
