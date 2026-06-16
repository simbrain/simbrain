package org.simbrain.network.subnetworks

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.core.*
import org.simbrain.network.events.SubnetworkEvents
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import java.awt.geom.Point2D

/**
 * A collection of [org.simbrain.network.NetworkModel] objects which functions as a subnetwork within the main
 * root network, which (1) is shown in the GUI with an outline around it and a custom interaction box and (2) has
 * a potentially custom update rule.
 * <br></br>
 * Subclasses use [.addModel] to add models, and subclass
 * [org.simbrain.network.gui.nodes.SubnetworkNode] to customize the presentation, and override NetworkModel
 * methods as needed for custom behavior.
 */
abstract class Subnetwork : LocatableModel(), EditableObject, AttributeContainer {

    @Transient
    override val events: SubnetworkEvents = SubnetworkEvents()

    val modelList: NetworkModelList = NetworkModelList()

    /**
     * Encodes all parent-child relationships between NetworkModels.
     * For example, each Neuron in a NeuronCollection is mapped to its parent NeuronCollection.
     * Needed for undo / redo.
     */
    val childToParentMap = mutableMapOf<NetworkModel, NetworkModel>()

    /**
     * Whether the GUI should display neuron collections contained in this subnetwork. This will usually be true, but in
     * cases where a subnetwork has just one neuron collection it is redundant to display both. So this flag indicates to the
     * GUI that neuron collections in this subnetwork need not be displayed.
     */
    private val displayNeuronGroups = true

    /**
     * Create a [NeuronCollection] and add both the neurons and the collection to this subnetwork's model list.
     * This mirrors the main network's pattern where neurons are free models tracked individually.
     */
    fun addNeuronCollection(neurons: List<Neuron>): NeuronCollection {
        neurons.forEach { addModel(it) }
        return NeuronCollection(neurons).also { addModel(it) }
    }

    /**
     * Register an already-created [NeuronCollection] with this subnetwork, adding both its neurons
     * (as free models) and the collection itself to the model list. Use this instead of [addModel]
     * when the collection's neurons are not yet tracked, e.g. in [copy]. Without this the neurons
     * get GUI nodes but are invisible to deletion, leaving leftover nodes on undo.
     */
    fun addNeuronCollection(neuronCollection: NeuronCollection): NeuronCollection {
        neuronCollection.neuronList.forEach { addModel(it) }
        addModel(neuronCollection)
        return neuronCollection
    }

    fun addModel(model: NetworkModel) {
        modelList.add(model)
        if (model is LocatableModel) {
            model.events.locationChanged.on(Dispatchers.Default) {
                events.locationChanged.fire()
            }
        }
        when(model) {
            is NeuronCollection -> {
                model.neuronList.forEach { childToParentMap[it] = model }
                model.neuronList.forEach { n ->
                    n.events.deleted.on(Dispatchers.Default) { childToParentMap.remove(n) }
                }
            }
            is SynapseGroup -> {
                model.synapses.forEach { childToParentMap[it] = model }
                model.synapses.forEach { s ->
                    s.events.deleted.on(Dispatchers.Default) { childToParentMap.remove(s) }
                }
            }
        }
        events.locationChanged.fire()
        model.events.deleted.on(Dispatchers.Default) {
            modelList.remove(it)
            childToParentMap.remove(it)
            if (modelList.size == 0) {
                delete()
            }
        }
    }

    fun addModels(models: List<NetworkModel>) {
        models.forEach { this.addModel(it) }
    }

    fun addModels(vararg models: NetworkModel) {
        for (model in models) {
            addModel(model)
        }
    }

    /**
     * Delete this subnetwork and its children.
     *
     * @return the subnetwork and its components, for undo/redo
     */
    override suspend fun delete(): List<NetworkModel> {
        // TODO: Should this be collecting the results of each models delete() function
        val toDelete = modelList.all.toList()
        toDelete.forEach {
            modelList.remove(it)
            it.delete()
        }
        customInfo?.let { it.events.deleted.fire(it) }
        events.deleted.fire(this)
        return toDelete + this
    }

    override suspend fun afterRestore(context: Any?) {
        modelList.allInUpdatingOrder.forEach {
            it.afterRestore()
        }
    }

    /**
     * A "flat" list containing every neuron in every neuron group in this subnetwork
     */
    val flatNeuronList: List<Neuron>
        get() = modelList[Neuron::class.java].toList()

    /**
     * A "flat" list containing every synapse in every synapse group in this subnetwork.
     */
    val flatSynapseList: List<Synapse>
        get() = modelList[Synapse::class.java].toList()

    override val name: String
        get() = "" + javaClass.simpleName

    /**
     * Default subnetwork update just updates all neuron and synapse groups. Subclasses with custom update should
     * override this.
     */
    context(Network)
    override fun update() {
        modelList.allInUpdatingOrder.forEach { it.update() }
    }

    override fun clear() {
        modelList.all.forEach { it.clear() }
    }

    private val locatableModels: List<LocatableModel>
        get() = modelList.all.filterIsInstance<LocatableModel>()

    override var location: Point2D
        get() = locatableModels.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            locatableModels.forEach { it.location += delta }
            customInfo?.let { it.location += delta }
        }

    /**
     * Optional information about the current state of the group. For display in GUI.
     */
    open val customInfo: LocatableModel? = null

    abstract fun copy(): Subnetwork
}
