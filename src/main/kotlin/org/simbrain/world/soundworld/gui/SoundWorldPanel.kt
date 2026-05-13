package org.simbrain.world.soundworld.gui

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.soundworld.SoundWorld
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Display panel for reading data from user and showing sound world's state.
 *
 * @author jyoshimi
 */
class SoundWorldPanel(val world: SoundWorld) : JPanel() {

    private var editor = createEditor()

    init {
        layout = BorderLayout()
        add(editor, BorderLayout.CENTER)
    }

    private fun createEditor() = AnnotatedPropertyEditor(listOf(world.generator), packWindowOnChange = false).also { editor ->
        editor.parameterWidgetMap.values.forEach { widget ->
            widget.events.valueChanged.on {
                editor.commitChanges()
            }
        }
    }

}
