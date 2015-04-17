package org.simbrain.world.threedee;

/**
 * Base class for implementing sensors in 3D world.
 *
 * @author Matt Watson
 */
public interface Sensor {
    /**
     * {@inheritDoc}
     * @return
     */
    Double getValue();

    /**
     * {@inheritDoc}
     * @return
     */
    String getDescription();
}
