package org.simbrain.world.odorworld.sensors;

import org.simbrain.world.odorworld.gui.EntityAttributeNode;

/**
 * Interface for entity attribute (sensor and effector) that has a visual representation in the OdorWorldPanel.
 */
public interface VisualizableEntityAttribute {

    /**
     * Get the visual representation of this attribute.
     *
     * @return the sensor node
     */
    EntityAttributeNode getNode();

}
