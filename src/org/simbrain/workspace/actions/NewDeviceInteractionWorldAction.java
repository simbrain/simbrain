package org.simbrain.workspace.actions;

import org.simbrain.resource.ResourceManager;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.deviceinteraction.DeviceInteractionComponent;

import java.awt.event.ActionEvent;

/**
 * @author Amanda Pandey
 */
public class NewDeviceInteractionWorldAction extends WorkspaceAction {

    /**
     *  Create new device interaction world action with the given workspace
     *
     * @param workspace
     */
    public NewDeviceInteractionWorldAction (Workspace workspace) {
        super("Device Interaction World", workspace);
        putValue(SHORT_DESCRIPTION, "New Keyboard World");
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Text.png"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DeviceInteractionComponent world = new DeviceInteractionComponent("");
        workspace.addWorkspaceComponent(world);

    }
}
