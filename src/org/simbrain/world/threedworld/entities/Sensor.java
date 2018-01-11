package org.simbrain.world.threedworld.entities;

/**
 * Sensor is an object which can sense some aspect of the ThreeDWorld for an Agent.
 * @author Tim Shea
 */
public interface Sensor {
    Agent getAgent();

    void update(float tpf);

    void delete();

    SensorEditor getEditor();
}
