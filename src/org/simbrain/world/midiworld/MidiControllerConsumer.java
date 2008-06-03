package org.simbrain.world.midiworld;

import promidi.Controller;

/**
 * MIDI controller consumer.
 */
final class MidiControllerConsumer
    extends AbstractMidiConsumer {

    /** MIDI controller. */
    private final Controller controller;


    /**
     * Create a new MIDI controller consumer with the specified controller and MIDI world workspace component.
     *
     * @param controller MIDI controller, must not be null
     * @param component MIDI world component, must not be null
     */
    MidiControllerConsumer(final Controller controller, final MidiWorldComponent component) {
        super(component);
        if (controller == null) {
            throw new IllegalArgumentException("controller must not be null");
        }
        this.controller = controller;
    }


    /** {@inheritDoc} */
    protected void trigger(final double value) {
        ((MidiWorldComponent) getParentComponent()).getMidiOut().sendController(controller);
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "MIDI Controller";
    }
}