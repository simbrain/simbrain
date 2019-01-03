package org.simbrain.world.odorworld.entities;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * Interface for effectors and sensors. "Peripheral" is supposed to suggest
 * the peripheral nervous system, which encompasses sensory and motor neurons.
 * It's the best I could come up with... :/
 *
 * @author Jeff Yoshimi
 */
public interface PeripheralAttribute extends AttributeContainer {


    public String getId();

    public String getLabel();

    public String getTypeDescription();

    public OdorWorldEntity getParent();

    public void setParent(OdorWorldEntity parent);

    public void setLabel(String label);

    /**
     * Called by reflection by some attributes.
     * @return
     */
    default String getMixedId() {
        return this.getParent().getId() + ":" + this.getId();
    }

    /**
     * Called by reflection to return a custom description for the {@link
     * org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer}
     * corresponding to object sensors and effectors.
     */
    default String getAttributeDescription() {
        String sensorEffector = (this instanceof Sensor) ? "Sensor" : "Effector";
        return getParent().getName() + ":" + getTypeDescription() + " " + sensorEffector;
    }
}
