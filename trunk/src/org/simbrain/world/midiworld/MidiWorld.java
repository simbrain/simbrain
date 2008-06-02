package org.simbrain.world.midiworld;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * MIDI world.
 */
public final class MidiWorld
    extends JPanel {

    /**
     * Create a new MIDI world.
     */
    public MidiWorld() {
        super();
        add("Center", new JLabel("MIDI World"));
    }
}