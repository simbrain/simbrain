package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import org.simbrain.workspace.Workspace;

import org.simbrain.world.midiworld.MidiWorldComponent;

/**
 * Add MIDI world to workspace.
 */
public final class NewMidiWorldAction extends WorkspaceAction {

    /**
     * Create a new MIDI world action with the specified workspace.
     *
     * @param workspace workspace
     */
    public NewMidiWorldAction(final Workspace workspace) {
        super("MIDI World", workspace);
    }

    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent event) {
        MidiWorldComponent world = new MidiWorldComponent("");
        workspace.addWorkspaceComponent(world);
    }
}