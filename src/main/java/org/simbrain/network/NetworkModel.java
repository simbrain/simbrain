package org.simbrain.network;


import org.jetbrains.annotations.NotNull;
import org.simbrain.network.events.NetworkModelEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

/**
 * "Model" objects placed in a {@link org.simbrain.network.core.Network} should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with "nodes" in the GUI which represent these objects.
 * <p>
 * Primarily meant as a marker interface.
 */
public abstract class NetworkModel {

    /**
     * A unique id for this model.
     */
    // TODO: Would be nice if this were final
    private String id;

    /**
     * Optional string description of neuron.
     */
    @UserParameter(label = "Label", description = "Optional string description",  useSetter = true, order = 2)
    private String label = "";

    /**
     * Update buffers
     */
    public void update() {
    }

    // TODO: Is this is correct most general method?
    //  No argument because for loose neurons it's not needed. But array conectables use setInputBuffer; a confusing
    // overlap of terminology

    /**
     * Set buffer values as part of async updating.  See {@link org.simbrain.network.update_actions.BufferedUpdate}
     */
    void setBufferValues() {}

    /**
     * Apply buffer values to actual values of models, to support async updating.
     */
    public void applyBufferValues() {}

    /**
     * Return a reference to that model type's instance of [NetworkModelEvent]
     */
    public abstract NetworkModelEvents getEvents();

    /**
     * Select this network model.
     */
    public void select() {
        getEvents().fireSelected();
    }

    /**
     * Override if cleanup is needed when deleting.
     */
    public void delete() {
    }

    ;

    // TODO: Discuss the methods below

    void afterAddedToNetwork() {
    }

    public void afterBatchAddedToNetwork() {
        afterAddedToNetwork();
    }

    public boolean shouldAdd() {
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(final String theName) {
        id = theName;
    }

    @Producible(defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        String oldLabel = this.label;
        this.label = label;
        if (this.label == null) {
            this.label = "";
        }
        getEvents().fireLabelChange(oldLabel, this.label);
    }

}
