package org.simbrain.world.oscworld;

import java.io.OutputStream;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * OSC world component.
 */
public final class OscWorldComponent
    extends WorkspaceComponent<WorkspaceComponentListener> {

    /**
     * Create a new OSC world component with the specified name.
     *
     * @param name name of this OSC world component
     */
    public OscWorldComponent(final String name) {
        super(name);
    }


    /** {@inheritDoc} */
    public void close() {
        // empty
    }

    /** {@inheritDoc} */
    public void save(final OutputStream outputStream, final String format) {
        // empty
    }

    /** {@inheritDoc} */
    public void update() {
        // empty
    }
}