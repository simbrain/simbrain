package org.simbrain.world.midiworld;

import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

import promidi.Controller;
import promidi.MidiIO;
import promidi.MidiOut;
import promidi.Note;
import promidi.ProgramChange;

/**
 * MIDI world component.
 */
public final class MidiWorldComponent
    extends WorkspaceComponent<WorkspaceComponentListener> {

    /** MIDI out. */
    private final MidiOut midiOut;

    /** List of MIDI consumers. */
    private final List<AbstractMidiConsumer> consumers;


    /**
     * Create a new MIDI world component with the specified name.
     *
     * @param name name of this MIDI world component
     */
    public MidiWorldComponent(final String name) {
        super(name);
        midiOut = MidiIO.getInstance().getMidiOut(0, 0);
        consumers = new ArrayList<AbstractMidiConsumer>();

        // TODO:  just for now...
        addNote(40, 80, 600);
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
     * Return the MIDI out for this MIDI world component.
     *
     * @return the MIDI out for this MIDI world component
     */
    MidiOut getMidiOut() {
        return midiOut;
    }

    /**
     * Add a MIDI controller with the specified number and value.  The controller
     * can be triggered via a coupling to this MIDI world component.
     *
     * @param number MIDI controller number
     * @param value MIDI controller value
     */
    void addController(final int number, final int value) {
        Controller controller = new Controller(number, value);
        MidiControllerConsumer consumer = new MidiControllerConsumer(controller, this);
        consumers.add(consumer);
        // fire event?
    }

    /**
     * Add a MIDI note with the specified pitch, velocity, and length.  The note
     * can be triggered via a coupling to this MIDI world component.
     *
     * @param pitch MIDI note pitch
     * @param velocity MIDI note velocity
     * @param length MIDI note length in milliseconds
     */
    void addNote(final int pitch, final int velocity, final int length) {
        Note note = new Note(pitch, velocity, length);
        MidiNoteConsumer consumer = new MidiNoteConsumer(note, this);
        consumers.add(consumer);
        // fire event?
    }

    /**
     * Add a MIDI program change with the specified number.  The program change
     * can be triggered via a coupling to this MIDI world component.
     *
     * @param number MIDI program change number
     */
    void addProgramChange(final int number) {
        ProgramChange programChange = new ProgramChange(number);
        MidiProgramChangeConsumer consumer = new MidiProgramChangeConsumer(programChange, this);
        consumers.add(consumer);
        // fire event?
    }
}