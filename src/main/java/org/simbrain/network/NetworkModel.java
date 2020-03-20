package org.simbrain.network;


/**
 * "Model" objects placed in a {@link org.simbrain.network.core.Network} should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with "nodes" in the GUI which represent these objects.
 * <p>
 * Primarily meant as a marker interface.
 */
public interface NetworkModel {

    String getLabel();

    void update();

}
