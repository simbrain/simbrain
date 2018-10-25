package org.simbrain.world.odorworld.entities;

/**
 * Interface for effectors and sensors. "Peripheral" is supposed to suggest
 * the peripheral nervous system, which encompasses sensory and motor neurons.
 * It's the best I could come up with... :/
 *
 * @author Jeff Yoshimi
 */
public interface PeripheralAttribute {


    public String getId();

    public String getLabel();

    public String getTypeDescription();

    public OdorWorldEntity getParent();

    public void setLabel(String label);


    // Called by reflection from some couplings
    default String getMixedId() {
        return this.getParent().getId() + ":" + this.getId();
    }
}
