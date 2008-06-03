package org.simbrain.world.oscworld;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * OSC world.
 */
public final class OscWorld
    extends JPanel {

    /**
     * Create a new OSC world.
     */
    public OscWorld() {
        super();
        add("Center", new JLabel("OSC World"));
    }
}