package org.simbrain.world.soundworld.gui

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.soundworld.SoundWorld
import javax.swing.JPanel

/**
 * Display panel for reading data from user and showing sound world's state.
 *
 * @author jyoshimi
 */
class SoundWorldPanel(val world: SoundWorld) : JPanel() {

    val editor = AnnotatedPropertyEditor(world.generator).also {
        add(it)
    }

}