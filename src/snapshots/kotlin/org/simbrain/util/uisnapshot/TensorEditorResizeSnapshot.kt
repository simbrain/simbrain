/**
 * The Data tab of a tensor layer's property editor after its dialog has been enlarged. The activation and bias
 * tables should take up the extra width and height instead of staying at their packed size.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.SwingUtilities

class TensorEditorResizeSnapshot : UiSnapshotDef {
    override val name = "tensor_editor_resize"

    override fun build(): Component {
        val shape = TensorShape(28, 28, 3)
        val tensor = TensorLayer(shape).apply {
            activations = DoubleArray(shape.size) { (it % 100) / 100.0 }
        }
        lateinit var content: JComponent
        SwingUtilities.invokeAndWait {
            val editor = AnnotatedPropertyEditor(listOf(tensor))
            editor.selectTab("Data")
            JDialog().apply {
                contentPane = editor
                pack()
                setSize(1000, 900)
                validate()
            }
            content = editor
        }
        return content
    }
}
