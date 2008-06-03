package org.simbrain.world.oscworld;

import org.simbrain.workspace.gui.DesktopComponent;

/**
 * OSC world desktop component.
 */
public final class OscWorldDesktopComponent
    extends DesktopComponent<OscWorldComponent> {

    /**
     * Create a new OSC world desktop component with the specified OSC world component.
     *
     * @param oscWorldComponent OSC world component
     */
    public OscWorldDesktopComponent(final OscWorldComponent oscWorldComponent) {
        super(oscWorldComponent);
        add("Center", new OscWorld());
    }


    /** {@inheritDoc} */
    public void close() {
        // empty
    }
}