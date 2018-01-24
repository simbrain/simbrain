package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.imageworld.ImageWorldComponent;

/**
 * Add ImageWorld to workspace.
 */
public class NewImageWorldAction extends WorkspaceAction {
    private static final long serialVersionUID = 1L;

    /**
     * Create a new ImageWorld action with the specified workspace.
     * @param workspace The workspace which will own new ImageWorlds.
     */
    public NewImageWorldAction(Workspace workspace) {
        super("Image World", workspace);
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("camera.png"));
        putValue(SHORT_DESCRIPTION, "Create an Image World");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        workspace.addWorkspaceComponent(new ImageWorldComponent());
    }
}
