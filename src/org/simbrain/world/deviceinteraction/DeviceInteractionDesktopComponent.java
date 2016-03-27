package org.simbrain.world.deviceinteraction;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class DeviceInteractionDesktopComponent extends
        GuiComponent<DeviceInteractionComponent> {

    /**
     * Construct a workspace component.
     *
     * @param frame              the parent frame.
     * @param workspaceComponent the component to wrap.
     */
    public DeviceInteractionDesktopComponent(GenericFrame frame, DeviceInteractionComponent workspaceComponent) {
        super(frame, workspaceComponent);
    }

    @Override
    protected void closing() {
        //no implementation
    }
}
