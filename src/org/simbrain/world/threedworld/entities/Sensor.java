package org.simbrain.world.threedworld.entities;

import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.AttributeContainer;

/**
 * Sensor is an object which can sense some aspect of the ThreeDWorld for an Agent.
 *
 * @author Tim Shea
 */
public interface Sensor extends EditableObject, AttributeContainer {
    String getName();

    Agent getAgent();

    void update(float tpf);

    void delete();
}
