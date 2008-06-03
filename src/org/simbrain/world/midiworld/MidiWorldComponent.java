package org.simbrain.world.midiworld;

import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import promidi.MidiIO;
import promidi.MidiOut;

/**
 * MIDI world component.
 */
public final class MidiWorldComponent
    extends WorkspaceComponent<WorkspaceComponentListener> {

    /** MIDI out. */
    private final MidiOut midiOut;

    /** List of MIDI note consumers. */
    private final List<MidiNoteConsumer> consumers;


    /**
     * Create a new MIDI world component with the specified name.
     *
     * @param name name of this MIDI world component
     */
    public MidiWorldComponent(final String name) {
        super(name);
        midiOut = MidiIO.getInstance().getMidiOut(0, 0);
        consumers = new ArrayList<MidiNoteConsumer>();
        // TODO: just as an example for now...
        consumers.add(new MidiNoteConsumer(new promidi.Note(40, 80, 600), this));
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

    /** {@inheritDoc} */
    public Collection<? extends Consumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    /**
     * Return the MIDI out  for this MIDI world component.
     *
     * @return the MIDI out for this MIDI world component
     */
    MidiOut getMidiOut() {
        return midiOut;
    }
}