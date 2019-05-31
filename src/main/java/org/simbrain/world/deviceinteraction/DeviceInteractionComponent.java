package org.simbrain.world.deviceinteraction;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */

//All coupling components go here

public class DeviceInteractionComponent extends WorkspaceComponent {

    private static final String KEYBOARD_DEVICE_INTERACTION_WORLD = "Keyboard Device Interaction World";

    private final KeyboardWorld world;

    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public DeviceInteractionComponent(String name) {
        super(name);
        this.world = new KeyboardWorld();
    }

    public DeviceInteractionComponent(String name, KeyboardWorld world) {
        super(name);
        this.world = world;
    }

    public static DeviceInteractionComponent open(InputStream input, final String name, final String format) {
        KeyboardWorld newWorld = (KeyboardWorld) KeyboardWorld.getXStream().fromXML(input);
        return new DeviceInteractionComponent(name, newWorld);
    }

    @Override
    public void save(OutputStream output, String format) {
        KeyboardWorld.getXStream().toXML(world, output);
    }

    @Override
    protected void closing() {
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        return Arrays.asList(world);
    }

    public KeyboardWorld getWorld() {
        return world;
    }
}
