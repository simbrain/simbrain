package org.simbrain.world.midiworld;

import promidi.Note;

/**
 * MIDI note consumer.
 */
final class MidiNoteConsumer
    extends AbstractMidiConsumer {

    /** MIDI note. */
    private final Note note;


    /**
     * Create a new MIDI note consumer with the specified note and MIDI world workspace component.
     *
     * @param note MIDI note, must not be null
     * @param component MIDI world component, must not be null
     */
    MidiNoteConsumer(final Note note, final MidiWorldComponent component) {
        super(component);
        if (note == null) {
            throw new IllegalArgumentException("note must not be null");
        }
        this.note = note;
    }


    /** {@inheritDoc} */
    protected void trigger(final double value) {
        ((MidiWorldComponent) getParentComponent()).getMidiOut().sendNote(note);
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "MIDI Note";
    }
}