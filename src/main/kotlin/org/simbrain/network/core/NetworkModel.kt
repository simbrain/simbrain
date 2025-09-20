package org.simbrain.network.core

import kotlinx.coroutines.runBlocking
import org.simbrain.network.events.NetworkModelEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
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
    var id: String? = null

    /**
     * Optional string description of model object.
     */
    @get:Producible(defaultVisibility = false)
    @set:Consumable(defaultVisibility = false)
    @UserParameter(label = "Label", description = "Optional string description", order = 2)
    var label: String? = null
        set(label) {
            val oldLabel = this.label
            field = label
            events.labelChanged.fire(oldLabel, this.label)
        }

    @UserParameter(label = "Update Priority", order = 20)
    var priority: Int = 0

    /**
     * First pass of updating. Generally a "weighted input".
     */
    context(Network)
    open fun accumulateInputs() {}

    /**
     * Update the state of the model, based in part on weighted inputs set in [.updateInputs]
     */
    context(Network)
    open fun update() {}

    /**
     * Return a reference to that model type's instance of [NetworkModelEvent]
     */
    abstract val events: NetworkModelEvents

    /**
     * Select this network model.
     */
    fun select() {
        events.selected.fire(this)
    }

    /**
     * Main public entry point for object deletion.
     * @return a list of NetworkModels deleted. Required because they must be added back if the delete action is undone.
     */
    open suspend fun delete(): List<NetworkModel> {
        // Do NOT create any public deletion methods in network, subnetwork, neurongroup, etc.
        // Deleting the object should fire an event and all cleanup should occur in response to those events.
        return emptyList()
    }

    /**
     * Perform required cleanup after restoring deleted objects as part of an undo or redo.
     */
    open suspend fun afterRestore(context: Any? = null) {}

    /**
     * For backward compatibility since suspend function cannot be called from Java
     */
    fun deleteBlocking() {
        runBlocking { delete() }
    }

    /**
     * Override if there are cases where a model should not be added after creation, e.g. if it is a
     * duplicate of an existing model. Currently only used by [org.simbrain.network.groups.NeuronCollection]
     */
    context(Network)
    open fun shouldAdd(): Boolean {
        return true
    }

    /**
     * Override to provide a means of randomizing a model. Custom randomizers can be passed in (usually by simulations)
     * but implementations generally also use reasonable defaults.
     */
    open fun randomize(randomizer: ProbabilityDistribution? = null) {}

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

    val displayName get() = label ?: id ?: "Uninitialized Network Model"

    fun commonCopyFrom(other: NetworkModel) {
        this.label = other.label
        this.priority = other.priority
    }

    override fun toString(): String = displayName
}