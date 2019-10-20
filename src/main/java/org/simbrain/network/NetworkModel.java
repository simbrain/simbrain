package org.simbrain.network;

/**
 * "Model" objects placed in a {@link org.simbrain.network.core.Network} should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with "nodes" in the GUI which represent these objects.
 */
public interface NetworkModel {

    public double getCenterX();

    public double getCenterY();

    public void setCenterX(double newx);

    public void setCenterY(double newy);

    // TODO: ScreenElement getModel should return these.
    // SimnetUtils.getUpperLeft
    // Clipboard.getPostPasteSelectionObjects
    // setx > setcenterx, etc.

    // TODO: Handle deletion here

}
