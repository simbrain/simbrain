package org.simbrain.world.midiworld;

import org.simbrain.workspace.gui.DesktopComponent;

/**
 * MIDI world desktop component.
 */
public final class MidiWorldDesktopComponent
    extends DesktopComponent<MidiWorldComponent> {    

    /**
     * Create a new MIDI world desktop component with the specified MIDI world component.
     *
     * @param midiWorldComponent MIDI world component
     */
    public MidiWorldDesktopComponent(final MidiWorldComponent midiWorldComponent) {
        super(midiWorldComponent);
        add("Center", new MidiWorld());
    }


    /** {@inheritDoc} */
    public void close() {
        // empty
    }
}