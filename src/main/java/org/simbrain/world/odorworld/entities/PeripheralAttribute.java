package org.simbrain.world.odorworld.entities;

import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.world.odorworld.events.AttributeEvents;

/**
 * Interface for effectors and sensors. "Peripheral" is supposed to suggest
 * the peripheral nervous system, which encompasses sensory and motor neurons.
 * It's the best I could come up with... :/
 *
 * @author Jeff Yoshimi
 */
public interface PeripheralAttribute extends AttributeContainer, CopyableObject {

    String getLabel();

    OdorWorldEntity getParent();

    void setParent(OdorWorldEntity parent);

    void setLabel(String label);

    AttributeEvents getEvents();

    /**
     * Called by reflection to return a custom description for the {@link
     * org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer}
     * corresponding to object sensors and effectors.
     */
    default String getAttributeDescription() {
        return getParent().getName() + ":" + getId() + ":" + getLabel();
    }
}
