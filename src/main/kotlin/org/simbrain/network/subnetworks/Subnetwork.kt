package org.simbrain.network.subnetworks

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
     * For example, each Neuron in a NeuronGroup is mapped to its to parent NeuronGroup
     * Needed for undo / redo.
     */
    val childToParentMap = mutableMapOf<NetworkModel, NetworkModel>()

    /**
     * Whether the GUI should display neuron groups contained in this subnetwork. This will usually be true, but in
     * cases where a subnetwork has just one neuron group it is redundant to display both. So this flag indicates to the
     * GUI that neuron groups in this subnetwork need not be displayed.
     */
    private val displayNeuronGroups = true

    fun addModel(model: NetworkModel) {
        modelList.add(model)
        if (model is LocatableModel) {
            model.events.locationChanged.on {
                events.locationChanged.fire()
            }
        }
        when(model) {
            is AbstractNeuronCollection -> {
                model.neuronList.forEach { childToParentMap[it] = model }
                model.neuronList.forEach { n ->
                    n.events.deleted.on(wait = true) { childToParentMap.remove(n) }
                }
            }
            is SynapseGroup -> {
                model.synapses.forEach { childToParentMap[it] = model }
                model.synapses.forEach { s ->
                    s.events.deleted.on(wait = true) { childToParentMap.remove(s) }
                }
            }
        }
        events.locationChanged.fire()
        model.events.deleted.on(wait = true) {
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
        events.deleted.fire(this).await()
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
