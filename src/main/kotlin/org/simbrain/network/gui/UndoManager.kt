package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
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

    // Snapshot the parent maps of every subnetwork in the network, not just the ones being deleted.
    // Deleting a single internal child (e.g. one neuron in a subnetwork's NeuronCollection, or one
    // synapse in its SynapseGroup) records the child->container link only in that subnetwork's own
    // childToParentMap. If that map is not snapshotted, restore cannot find the child's parent, treats
    // it as a free top-level model, and re-adds it incorrectly (which also deadlocks node creation).
    private val subnetworks: List<Subnetwork> = buildList {
        fun collect(subnet: Subnetwork) {
            add(subnet)
            subnet.modelList.all.filterIsInstance<Subnetwork>().forEach { collect(it) }
        }
        network.getModels<Subnetwork>().forEach { collect(it) }
    }

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
                    // The awaited addNetworkModel above already created the neuron's node, so a non-blocking
                    // peek finds it; recreate only if it is somehow absent. Attach to the collection node.
                    (modelNodeMap.peek(parent) as? NeuronCollectionNode)?.let { collectionNode ->
                        val neuronNode = (modelNodeMap.peek(neuron) as? NeuronNode) ?: createNode(neuron)
                        collectionNode.addNeuronNodes(listOf(neuronNode))
                    }
                }
            }

            is SynapseGroup -> {
                (model as? Synapse)?.let { synapse ->
                    parent.synapses.add(synapse)
                    // Restoring synapses changes the group's size; recompute its expanded/collapsed state.
                    // A flip to collapsed fires visibilityChanged, whose reconcile removes any loose nodes.
                    parent.refreshVisibility()
                    // Recreate a loose SynapseNode only when the group is expanded AND its node and both
                    // endpoint nodes are already live. Gating with non-blocking peeks avoids two hazards
                    // during a full-subnetwork redo: a stale group node left by an in-flight async removal
                    // must not spawn synapse nodes (the group rebuilds via createNode(subnetwork)), and
                    // createNode(synapse) must never block on an endpoint recreated later in this restore.
                    if (parent.displaySynapses &&
                        modelNodeMap.peek(parent) != null &&
                        modelNodeMap.peek(synapse.source) != null &&
                        modelNodeMap.peek(synapse.target) != null
                    ) {
                        createNode(synapse)
                        synapse.isVisible = parent.displaySynapses
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
                val collection = (model as? Neuron)?.let { parent.childToParentMap[it] as? NeuronCollection }
                if (collection != null && model !in collection.neuronList) {
                    collection.neuronList.add(model)
                }
                // Re-create the node if its asynchronous, debounced removal already landed (getImmediately
                // returns null once it has), then attach it to the right container node. Without this a
                // restored internal model (e.g. a neuron array in a feedforward) comes back as a model but
                // stays invisible on the canvas. createNode for these layer/connector/neuron types does not
                // await any other node, so re-creating them here cannot deadlock; for any other type fall
                // back to re-attaching an already-live node, never blocking on an endpoint that is only
                // re-created later in this same restore.
                if (collection != null) {
                    modelNodeMap.getImmediately<NeuronCollectionNode>(collection)?.let { collectionNode ->
                        val neuronNode = modelNodeMap.getImmediately<NeuronNode>(model) ?: createNode(model as Neuron)
                        collectionNode.addNeuronNodes(listOf(neuronNode))
                    }
                } else {
                    // Non-blocking peeks: restore must not stall up to 1s awaiting a pending node. The
                    // subnetwork node is already live for a surviving subnetwork (skip the attach if not),
                    // and the model's own node was removed on delete, so peek returns null and we recreate
                    // it below (createNode also completes any pending waiter on that model's node).
                    (modelNodeMap.peek(parent) as? SubnetworkNode)?.let { subnetworkNode ->
                        val screenElement = modelNodeMap.peek(model) ?: when (model) {
                            is NeuronArray -> createNode(model)
                            is NeuronCollection -> createNode(model)
                            is TensorLayer -> createNode(model)
                            is TensorConnector -> createNode(model)
                            is FlattenConnector -> createNode(model)
                            is Connector -> createNode(model)
                            // A SynapseGroup restored into a surviving subnetwork has live endpoint
                            // collections, so createNode(SynapseGroup) (which below the visibility
                            // threshold builds its synapse nodes) does not block on a recreated endpoint.
                            is SynapseGroup -> createNode(model)
                            else -> null
                        }
                        screenElement?.let { subnetworkNode.addNode(it) }
                    }
                }
            }

            is SupervisedModel -> {
                network.addNetworkModel(model, usePlacementManager = false, useAutoAssignedId = false)
                // Non-blocking: the overlay's own node is (re)built later in this restore via
                // createNode(SupervisedModel), which re-attaches its layer/matrix nodes; this best-effort
                // attach only matters when that node already exists, so peek-and-skip is correct.
                (modelNodeMap.peek(parent) as? SupervisedModelNode)?.let { supervisedModelNode ->
                    modelNodeMap.peek(model)?.let { screenElement ->
                        supervisedModelNode.addNode(screenElement)
                    }
                }
            }
        }
    }

    private fun hasNoParent(model: NetworkModel): Boolean {
        return childToParentMaps.none { it.containsKey(model) }
    }

    private fun immediateParent(model: NetworkModel): NetworkModel? =
        childToParentMaps.firstNotNullOfOrNull { it[model] }

    context(NetworkPanel)
    suspend fun restore(deletedModels: List<NetworkModel>) {
        restoreMapSnapshot()
        // Node removal on delete is asynchronous and debounced, so a redo running back-to-back with undo
        // can find the deleted models still carrying their old nodes (mapped and on the canvas). Node
        // recreation below reuses mapped nodes (getImmediately ?: createNode); reusing a node whose
        // removal is still pending leaves the model with no canvas node once that removal finally lands.
        // Proactively drop those stale nodes so recreation starts from a clean slate; the pending async
        // removal then resolves to an identity-safe no-op.
        val staleNodes = deletedModels.mapNotNull { model -> modelNodeMap.peek(model)?.let { model to it } }
        if (staleNodes.isNotEmpty()) {
            withContext(Dispatchers.Swing) { staleNodes.forEach { (_, node) -> canvas.layer.removeChild(node) } }
            staleNodes.forEach { (model, node) -> modelNodeMap.removeIfValue(model) { it === node } }
        }
        val modelsToReAdd = LinkedHashSet(deletedModels.reversed())
        // Adds models back to parent groups.
        modelsToReAdd.forEach { reAddToGroup(it) }
        // Add all models without parents back
        network.addNetworkModelsAsync(
            modelsToReAdd.filter { hasNoParent(it) },
            usePlacementManager = false,
            useAutoAssignedId = false
        ).awaitAll()
        // Finalize recreation. afterRestore re-establishes a model's external links (a Connector
        // re-registers with its endpoint layers, a Synapse with its neurons' fan-in/out, a SynapseGroup
        // with its layers, etc.). Call it on every restored model whose parent is NOT itself being
        // restored: parentless models, and models re-added into a container that survived the deletion
        // (e.g. one weight matrix put back into an existing subnetwork). Children whose parent is also
        // restored are finalized by that parent's afterRestore (Subnetwork/SupervisedModel recurse), so
        // they are skipped here to avoid double-registration (afterRestore is not idempotent).
        modelsToReAdd
            .filter { val parent = immediateParent(it); parent == null || parent !in modelsToReAdd }
            .forEach { it.afterRestore() }
    }

}