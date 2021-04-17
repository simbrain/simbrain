package org.simbrain.network;


import org.simbrain.network.events.NetworkModelEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

/**
 * "Model" objects placed in a {@link org.simbrain.network.core.Network} should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with GUI "nodes" which graphically represent these objects.
 */
public abstract class NetworkModel {

    /**
     * A unique id for this model.
     */
    // TODO: Would be nice if this were final
    private String id;

    /**
     * Optional string description of model object.
     */
    @UserParameter(label = "Label", description = "Optional string description",  useSetter = true, order = 2)
    private String label = "";

    /**
     * First pass of updating. Generally a "weighted input".
     */
    public void updateInputs() {}

    /**
     * Update the state of the model, based in part on weighted inputs set in {@link #updateInputs()}
     */
    public void update() {}

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

    /**
     * Override if there are cases where a model should not be added after creation, e.g. if it is a
     * duplicate of an existing model. Currently only used by {@link org.simbrain.network.groups.NeuronCollection}
     */
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
