package org.simbrain.workspace.actions;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.oscworld.OscWorldComponent;

import java.awt.event.ActionEvent;

/**
 * Add OSC world to workspace.
 */
public final class NewOscWorldAction extends WorkspaceAction {

    /**
     * Create a new OSC world action with the specified workspace.
     *
     * @param workspace workspace
     */
    public NewOscWorldAction(final Workspace workspace) {
        super("OSC World", workspace);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent event) {
        OscWorldComponent world = new OscWorldComponent("");
        workspace.addWorkspaceComponent(world);
    }
}