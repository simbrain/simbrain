package org.simbrain.world.midiworld;

import promidi.ProgramChange;

/**
 * MIDI program change consumer.
 */
final class MidiProgramChangeConsumer
    extends AbstractMidiConsumer {

    /** MIDI program change. */
    private final ProgramChange programChange;


    /**
     * Create a new MIDI program change consumer with the specified program change
     * and MIDI world workspace component.
     *
     * @param programChange MIDI program change, must not be null
     * @param component MIDI world component, must not be null
     */
    MidiProgramChangeConsumer(final ProgramChange programChange, final MidiWorldComponent component) {
        super(component);
        if (programChange == null) {
            throw new IllegalArgumentException("programChange must not be null");
        }
        this.programChange = programChange;
    }


    /** {@inheritDoc} */
    protected void trigger(final double value) {
        ((MidiWorldComponent) getParentComponent()).getMidiOut().sendProgramChange(programChange);
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "MIDI Program Change";
    }
}