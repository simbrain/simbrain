package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.threedworld.ThreeDWorldComponent;

import javax.swing.*;

/**
 * Add 3d world to workspace.
 */
public class NewThreeDWorldAction extends WorkspaceAction {
    private static final long serialVersionUID = 1L;

    private Workspace workspace;

    /**
     * Construct a new NewThreeDWorldAction.
     * @param workspace The workspace in which the action will create ThreeDWorlds
     */
    public NewThreeDWorldAction(Workspace workspace) {
        super("3D World", workspace);
        this.workspace = workspace;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("World.png"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (workspace.getComponentList(ThreeDWorldComponent.class).isEmpty()) {
            ThreeDWorldComponent worldComponent = new ThreeDWorldComponent("3D World");
            workspace.addWorkspaceComponent(worldComponent);
        } else {
            JOptionPane.showMessageDialog(null, "Only one component of this type is supported.");
        }
    }
}
