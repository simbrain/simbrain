package org.simbrain.world.speechsynthesizer.gui

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.world.speechsynthesizer.SpeechSynthesizerComponent

class SpeechSynthesizerDesktopComponent(frame: GenericFrame, component: SpeechSynthesizerComponent) :
    DesktopComponent<SpeechSynthesizerComponent>(frame, component) {

    private val panel = SpeechSynthesizerPanel(component.synthesizer)

    init {
        frame.title = component.name
        add(panel)
        frame.pack()
    }
}
