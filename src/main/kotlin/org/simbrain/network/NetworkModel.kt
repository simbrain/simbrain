package org.simbrain.network

import org.simbrain.network.events.NetworkModelEvents
import org.simbrain.util.UserParameter
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible

/**
 * "Model" objects placed in a [org.simbrain.network.core.Network] should implement this interface.  E.g. neurons, synapses, neuron groups, etc.
 * Contrasted with GUI "nodes" which graphically represent these objects.
 */
abstract class NetworkModel {
    /**
     * A unique id for this model.
     */
    // TODO: Would be nice if this were final
    open var id: String? = null

    /**
     * Optional string description of model object.
     */
    @get:Producible(defaultVisibility = false)
    @set:Consumable(defaultVisibility = false)
    @UserParameter(label = "Label", description = "Optional string description", useSetter = true, order = 2)
    var label: String? = ""
        set(label) {
            val oldLabel = this.label
            field = label
            if (this.label == null) {
                field = ""
            }
            events.fireLabelChange(oldLabel!!, this.label!!)
        }

    /**
     * First pass of updating. Generally a "weighted input".
     */
    open fun updateInputs() {}

    /**
     * Update the state of the model, based in part on weighted inputs set in [.updateInputs]
     */
    open fun update() {}

    /**
     * Return a reference to that model type's instance of [NetworkModelEvent]
     */
    abstract val events: NetworkModelEvents

    /**
     * Select this network model.
     */
    fun select() {
        events.fireSelected()
    }

    /**
     * Main public entry point for object deletion.
     */
    open fun delete() {
        // Do NOT create any public deletion methods in network, subnetwork, neurongroup, etc.
        // Deleting the object should fire an event and all cleanup should occur in response to those events.
    }

    /**
     * Override if there are cases where a model should not be added after creation, e.g. if it is a
     * duplicate of an existing model. Currently only used by [org.simbrain.network.groups.NeuronCollection]
     */
    open fun shouldAdd(): Boolean {
        return true
    }

    /**
     * Override if custom unmarashalling is needed after the parent network is added. Often an event object is needed.
     * See overrides for examples.
     */
    open fun postOpenInit() {}

    /**
     * Override to provide a means of randomizing a model.
     */
    open fun randomize() {}

    /**
     * Override to provide a means of "clearing" a model.
     */
    open fun clear() {}

    /**
     * Override to provide a means of incrementing a model
     */
    open fun increment() {}

    /**
     * Override to provide a means of decrementing a model.
     */
    open fun decrement() {}

    /**
     * Override to provide a means of clamping and unclamping a model.
     */
    open fun toggleClamping() {}
}