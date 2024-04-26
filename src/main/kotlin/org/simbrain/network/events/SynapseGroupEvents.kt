package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.util.Events

/**
 * See [Events]
 */
class SynapseGroupEvents: NetworkModelEvents() {
    val visibilityChanged = NoArgEvent()
    val synapseAdded = OneArgEvent<Synapse>()
    val synapseRemoved = OneArgEvent<Synapse>()
    val synapseListChanged = NoArgEvent()
}