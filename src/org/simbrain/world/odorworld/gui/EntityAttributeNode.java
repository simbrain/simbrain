package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.sensors.*;

import java.awt.*;

/**
 * Interface for visual representation of entity attributes (sensors and effectors).
 */
public abstract class EntityAttributeNode extends PNode {

    /**
     * The color to show when the output value of the attribute is at max.
     */
    static float maxColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /**
     * Update the visual representation base on the attribute status.
     */
    public abstract void update();

    /**
     * Get the visual representation of a attribute.
     *
     * @param visualizableEntityAttribute the attribute to represent
     * @return the sensor node
     */
    static EntityAttributeNode getNode(VisualizableEntityAttribute visualizableEntityAttribute) {
        if (visualizableEntityAttribute instanceof BumpSensor) {
            return new BumpSensorNode((BumpSensor) visualizableEntityAttribute);
        }
        if (visualizableEntityAttribute instanceof Hearing) {
            return new HearingNode((Hearing) visualizableEntityAttribute);
        }
        if (visualizableEntityAttribute instanceof SmellSensor) {
            return new SmellSensorNode((SmellSensor) visualizableEntityAttribute);
        }
        if (visualizableEntityAttribute instanceof Speech) {
            return new SpeechNode((Speech) visualizableEntityAttribute);
        }
        if (visualizableEntityAttribute instanceof ObjectSensor) {
            return new ObjectSensorNode((ObjectSensor) visualizableEntityAttribute);
        }
        return null;
    }

}
