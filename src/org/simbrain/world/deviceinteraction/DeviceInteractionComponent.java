package org.simbrain.world.deviceinteraction;

import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */

//All coupling components go here

public class DeviceInteractionComponent extends WorkspaceComponent {


    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public DeviceInteractionComponent(String name) {
        super(name);
    }

    public static DeviceInteractionComponent open (
            InputStream input, final String name, final String format) {
      return new DeviceInteractionComponent(name);
    }

    @Override
    public void save(OutputStream output, String format) {
        //TODO implement
    }

    @Override
    protected void closing() {
        //No implementation
    }
}
