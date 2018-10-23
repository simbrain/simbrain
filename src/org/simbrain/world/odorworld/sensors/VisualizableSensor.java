package org.simbrain.world.odorworld.sensors;

import org.simbrain.world.odorworld.gui.SensorNode;

/**
 * Interface for sensor that has a visual representation in the OdorWorldPanel.
 */
public interface VisualizableSensor {

    /**
     * Get the visual representation of this sensor.
     *
     * @return the sensor node
     */
    SensorNode getNode();

}
