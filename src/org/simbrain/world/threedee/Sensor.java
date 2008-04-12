package org.simbrain.world.threedee;

import org.simbrain.workspace.Producer;

/**
 * Base class for implementing sensors in 3D world.
 * 
 * @author Matt Watson
 */
public interface Sensor {
    /**
     * {@inheritDoc}
     */
    Double getValue();

    /**
     * {@inheritDoc}
     */
    String getDescription();
}
