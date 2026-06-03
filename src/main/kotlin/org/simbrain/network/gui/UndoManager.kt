package org.simbrain.network.gui

import kotlinx.coroutines.awaitAll
import org.simbrain.network.core.*
import org.simbrain.network.gui.UndoManager.UndoableAction
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import java.util.*

/**
 * Manage undo / redo operations in the network panel.
 *
 * Related code can be found in callers to [addUndoableAction].
 *
 * [Network.deleteModels] is used quite a bit because undo/redo often involves deleting models.
 *
 * Also see [NetworkModel.afterRestore]
 *
 */
class UndoManager {

    /**
     * All actions that can be undone are pushed to this stack.
     */
    val undoStack = Stack<UndoableAction>()

    /**
     * When an action is undone, it is popped off the undo stack and pushed on
     * to this stack.
     */
    val redoStack = Stack<UndoableAction>()

    fun addUndoableAction(action: UndoableAction) {
        undoStack.push(action)
        redoStack.removeAllElements()
    }

    fun addUndoableAction(
        description: String,
        undo: suspend () -> Unit,
        redo: suspend () -> Unit
    ) {
        addUndoableAction(undoableAction(description, undo, redo))
    }

    /**
     * Undo the last undoable action.
     */
    suspend fun undo() {
        if (!undoStack.isEmpty()) {
            val lastEvent = undoStack.pop()
            lastEvent.undo()
            redoStack.push(lastEvent)
        }
    }

    /**
     * Redo the last undone action.
     */
    suspend fun redo() {
        if (!redoStack.isEmpty()) {
            val redoEvent = redoStack.pop()
            redoEvent.redo()
            undoStack.push(redoEvent)
        }
    }

    interface UndoableAction {
        val description: String
        suspend fun undo()
        suspend fun redo()
    }

}

fun undoableAction(
    description: String,
    undo: suspend () -> Unit,
    redo: suspend () -> Unit) =
    object : UndoableAction {

        override val description = description

        override suspend fun undo() {
            undo()
        }

        override suspend fun redo() {
            redo()
        }
    }


class UndeleteContext(val networkPanel: NetworkPanel, modelsToDelete: List<NetworkModel>) {

    private val network = networkPanel.network
    private val modelNodeMap get() = networkPanel.modelNodeMap

    private val modelsToDelete = modelsToDelete.sortedBy { updatingOrder(it) }

    private val subnetworks = this.modelsToDelete.filterIsInstance<Subnetwork>()

    // Snapshot of deleted objects and their relationships, which can be used to reconstruct a
    // prior state of the network
    private val childToParentMaps = listOf(network.childToParentMap) + subnetworks.map { it.childToParentMap }
    private val childToParentMapsSnapshots = childToParentMaps.map { it.toMap() }
    private fun restoreMapSnapshot() {
        childToParentMaps.zip(childToParentMapsSnapshots).forEach { (map, snapshot) ->
            map.clear()
            map.putAll(snapshot)
        }
    }

    // When undoing deletion of a group, its children must be re-added
    context(NetworkPanel)
    private suspend fun reAddToGroup(model: NetworkModel) {
        when (val parent = childToParentMaps.firstNotNullOfOrNull { it[model] }) {
            is NeuronCollection -> {
                (model as? Neuron)?.let { neuron ->
                    network.addNetworkModel(neuron, usePlacementManager = false, useAutoAssignedId = false)
                    parent.neuronList.add(neuron)
                    // If the node exists, create neuron nodes for the children and re-add them
                    modelNodeMap.getImmediately<NeuronCollectionNode>(parent)?.let { collectionNode ->
                        val neuronNode = modelNodeMap.getImmediately<NeuronNode>(neuron) ?: createNode(neuron)
                        collectionNode.addNeuronNodes(listOf(neuronNode))
                    }
                }
            }

            is SynapseGroup -> {
                (model as? Synapse)?.let { synapse ->
                    parent.synapses.add(synapse)
                    // If there is a synapsegroupnode already, we are deleting the synapse and must add the node back
                    // Note that synapsegroupnodes don't actually contain synapsenodes as children, so all we have to
                    // do is create the node. The check is still required, to synchronize with neuron group recreation.
                    modelNodeMap.getImmediately<SynapseGroupNode>(parent)?.let { synapseGroupNode ->
                        createNode(synapse)
                    }
                }
            }

            is Subnetwork -> {
                parent.modelList.add(model)
                // Deleting a subnetwork deletes its neurons individually, which empties any
                // NeuronCollection's neuronList. Restore that membership so the subnetwork's
                // collections (and their update logic and node grouping) work after redo. The
                // collection's own per-neuron deletion listener persists, so a plain add (rather
                // than addNeuron) avoids registering a duplicate listener.
                if (model is Neuron) {
                    (parent.childToParentMap[model] as? NeuronCollection)?.let { collection ->
                        if (model !in collection.neuronList) collection.neuronList.add(model)
                    }
                }
                modelNodeMap.getImmediately<SubnetworkNode>(parent)?.let { subnetworkNode ->
                    modelNodeMap.getImmediately<ScreenElement>(model)?.let { screenElement ->
                        subnetworkNode.addNode(screenElement)
                    }
                }
            }

            is SupervisedModel -> {
                network.addNetworkModel(model, usePlacementManager = false, useAutoAssignedId = false)
                modelNodeMap.getImmediately<SupervisedModelNode>(parent)?.let { supervisedModelNode ->
                    modelNodeMap.getImmediately<ScreenElement>(model)?.let { screenElement ->
                        supervisedModelNode.addNode(screenElement)
                    }
                }
            }
        }
    }

    private fun hasNoParent(model: NetworkModel): Boolean {
        return childToParentMaps.none { it.containsKey(model) }
    }

    context(NetworkPanel)
    suspend fun restore(deletedModels: List<NetworkModel>) {
        restoreMapSnapshot()
        val modelsToReAdd = LinkedHashSet(deletedModels.reversed())
        // Adds models back to parent groups.
        modelsToReAdd.forEach { reAddToGroup(it) }
        // Add all models without parents back
        network.addNetworkModelsAsync(
            modelsToReAdd.filter { hasNoParent(it) },
            usePlacementManager = false,
            useAutoAssignedId = false
        ).awaitAll()
        // Call afterRestore on all models to finalize recreation as needed
        modelsToReAdd.filter { hasNoParent(it) }.forEach { it.afterRestore() }
    }

}