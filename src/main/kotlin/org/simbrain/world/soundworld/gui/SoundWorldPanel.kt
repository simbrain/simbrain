package org.simbrain.world.soundworld.gui

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.soundworld.PhonemeSynthesizer
import org.simbrain.world.soundworld.SoundWorld
import org.simbrain.world.soundworld.warnIfEspeakUnavailable
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
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
        if (world.generator is PhonemeSynthesizer) {
            warnIfEspeakUnavailable()
            add(JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(JButton("Restore defaults").apply {
                    addActionListener {
                        (world.generator as PhonemeSynthesizer).restoreDefaults()
                        rebuildEditor()
                    }
                })
            }, BorderLayout.SOUTH)
        }
    }

    private fun createEditor() = AnnotatedPropertyEditor(listOf(world.generator), packWindowOnChange = false).also { editor ->
        editor.parameterWidgetMap.values.forEach { widget ->
            widget.events.valueChanged.on {
                editor.commitChanges()
            }
        }
    }

    private fun rebuildEditor() {
        remove(editor)
        editor = createEditor()
        add(editor, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

}
