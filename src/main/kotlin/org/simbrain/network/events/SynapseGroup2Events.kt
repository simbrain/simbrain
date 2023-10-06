package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.util.Events

/**
 * See [Events]
 */
class SynapseGroup2Events: NetworkModelEvents() {
    val visibilityChanged = NoArgEvent()
    val synapseAdded = AddedEvent<Synapse>()
    val synapseRemoved = RemovedEvent<Synapse>()
    val synapseListChanged = NoArgEvent()
}