package org.simbrain.network;


/**
 * "Model" objects placed in a {@link org.simbrain.network.core.Network} should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with "nodes" in the GUI which represent these objects.
 * <p>
 * Primarily meant as a marker interface.
 */
public interface NetworkModel {

    String getLabel();

    /**
     * Update buffers
     */
    void update();

    // TODO: Is this is correct most general method?
    //  No argument because for loose neurons it's not needed. But array conectables use setInputBuffer; a confusing
    // overlap of terminology
    /**
     * Set buffer values as part of async updating.  See {@link org.simbrain.network.update_actions.BufferedUpdate}
     */
    void setBufferValues();

    /**
     * Apply buffer values to actual values of models, to support async updating.
     */
    void applyBufferValues();

}
