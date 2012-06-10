package org.simbrain.world.midiworld;

import java.awt.Dimension;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * MIDI world desktop component.
 */
public final class MidiWorldDesktopComponent extends
        GuiComponent<MidiWorldComponent> {

    /**
     * Create a new MIDI world desktop component with the specified MIDI world
     * component.
     *
     * @param midiWorldComponent MIDI world component
     */
    public MidiWorldDesktopComponent(GenericFrame frame,
            final MidiWorldComponent midiWorldComponent) {
        super(frame, midiWorldComponent);
        add("Center", new MidiWorld());
        this.setPreferredSize(new Dimension(100, 100));
    }

    /** {@inheritDoc} */
    public void closing() {
        // empty
    }
}