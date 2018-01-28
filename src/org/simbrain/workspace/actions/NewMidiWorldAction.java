package org.simbrain.workspace.actions;

import org.simbrain.workspace.Workspace;

import java.awt.event.ActionEvent;

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

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent event) {
        //        MidiWorldComponent world = new MidiWorldComponent("");
        //        workspace.addWorkspaceComponent(world);
    }
}