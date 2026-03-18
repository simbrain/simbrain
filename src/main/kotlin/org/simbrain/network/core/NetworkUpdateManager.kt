package org.simbrain.network.core

import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.update_actions.BufferedUpdate
import org.simbrain.network.update_actions.PriorityUpdate
import org.simbrain.network.update_actions.UpdateNetworkModel
import org.simbrain.util.UpdateAction
import java.util.*

/**
 * Manage network updates. Maintains a list of actions that are updated in the
 * order in which they appear in the list when the network is iterated once (in
 * the GUI, when the step button is clicked).
 *
 * @author Jeff Yoshimi
 */
class NetworkUpdateManager(private val network: Network) {
    /**
     * The list of update actions, in a specific order. One run through these
     * actions constitutes a single "update" in the network.
     */
    private val _actionList: MutableList<UpdateAction> = ArrayList()

    val actionList: List<UpdateAction> get() = _actionList.toList()

    /**
     * Construct a new update manager.
     */
    init {
        // By default, only do a buffered update
        addAction(BufferedUpdate(network))
    }

    /**
     * This is the list of actions that are available to be added manually.
     */
    val availableActionList: List<UpdateAction>
        get() {
            // TODO: If added, these should be removed when any corresponding object is removed
            val actionableModels = with(network) {
                listOf(
                    getModels<NeuronCollection>(),
                    getModels<NeuronCollection>(),
                    getModels<Subnetwork>(),
                    getModels<SynapseGroup>(),
                    getModels<NeuronArray>(),
                    getModels<WeightMatrix>(),
                ).flatten()
            }

            val availableActionList = buildList {
                // By default these actions are always available
                add(BufferedUpdate(network))
                add(PriorityUpdate(network))

                addAll(actionableModels.map { UpdateNetworkModel(it, network) })
            }

            return availableActionList
        }

    /**
     * Swap elements at the specified location.
     *
     * @param index1 index of first element
     * @param index2 index of second element
     */
    fun swapElements(index1: Int, index2: Int) {
        Collections.swap(_actionList, index1, index2)
        network.events.updateActionsChanged.fire()
    }

    /**
     * Add the specified action to the update manager.
     */
    fun addAction(action: UpdateAction) {
        _actionList.add(action)
        network.events.updateActionsChanged.fire()
    }

    /**
     * Add an action at a specified position, i.e. update order.
     */
    fun addAction(index: Int, action: UpdateAction) {
        _actionList.add(index, action)
    }

    /**
     * Remove the specified action from the update manager.
     */
    fun removeAction(action: UpdateAction?) {
        _actionList.remove(action)
        network.events.updateActionsChanged.fire()
    }

    /**
     * Remove all actions completely.
     */
    fun clear() {
        _actionList.clear()
        network.events.updateActionsChanged.fire()
    }
}
