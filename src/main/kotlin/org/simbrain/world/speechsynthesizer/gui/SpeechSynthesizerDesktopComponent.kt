package org.simbrain.world.speechsynthesizer.gui

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.world.speechsynthesizer.SpeechSynthesizerComponent
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JScrollPane

class SpeechSynthesizerDesktopComponent(frame: GenericFrame, component: SpeechSynthesizerComponent) :
    DesktopComponent<SpeechSynthesizerComponent>(frame, component) {

    private val panel = SpeechSynthesizerPanel(component.synthesizer)

    init {
        frame.title = component.name
        layout = BorderLayout()
        // Scrolls when the settings section is opened in a window too short to show it
        add(JScrollPane(panel).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }, BorderLayout.CENTER)
        frame.pack()
    }
}
