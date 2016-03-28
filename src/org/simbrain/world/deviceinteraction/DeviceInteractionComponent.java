package org.simbrain.world.deviceinteraction;

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.WorkspaceComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */

//All coupling components go here

public class DeviceInteractionComponent extends WorkspaceComponent {

    private final KeyboardWorld world;

    private static final String KEYBOARD_DEVICE_INTERACTION_WORLD = "Keyboard Device Interaction World";


    /**
     * Construct a workspace component.
     *
     * @param name The name of the component.
     */
    public DeviceInteractionComponent(String name) {
        super(name);
        this.world = new KeyboardWorld();
        init();
    }

    public DeviceInteractionComponent(String name, KeyboardWorld world) {
        super(name);
        this.world = world;
        init();
    }

    public static DeviceInteractionComponent open (
            InputStream input, final String name, final String format) {
        KeyboardWorld newWorld = (KeyboardWorld) KeyboardWorld.getXStream()
                .fromXML(input);
        return new DeviceInteractionComponent(name, newWorld);
    }

    private void init() {
        addConsumerType(new AttributeType(this, KEYBOARD_DEVICE_INTERACTION_WORLD, double.class, true));
    }

    @Override
    public void save(OutputStream output, String format) {
        KeyboardWorld.getXStream().toXML(world, output);
    }

    @Override
    protected void closing() {
        //No implementation
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {
        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();
        for (AttributeType type : getVisibleConsumerTypes()) {

            if (type.getTypeName().equalsIgnoreCase(KEYBOARD_DEVICE_INTERACTION_WORLD)) {
                for (Character character : world.getTokenDictionary()) {
                    PotentialConsumer consumer = getAttributeManager()
                            .createPotentialConsumer(
                                    world,
                                    "keyPress",
                                    new Class<?>[] {double.class, Character.class},
                                    new Object[] {character});
                    consumer.setCustomDescription(String.valueOf(character));
                    returnList.add(consumer);
                }
            }
        }
        return returnList;
    }

    public KeyboardWorld getWorld() {
        return world;
    }
}
