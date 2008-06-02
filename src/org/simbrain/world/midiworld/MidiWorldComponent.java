package org.simbrain.world.midiworld;

import java.io.OutputStream;

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * MIDI world component.
 */
public final class MidiWorldComponent
    extends WorkspaceComponent<WorkspaceComponentListener> {

    /**
     * Create a new MIDI world component with the specified name.
     *
     * @param name name of this MIDI world component
     */
    public MidiWorldComponent(final String name) {
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