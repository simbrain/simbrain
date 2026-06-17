package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
class SynapseGroupEvents: NetworkModelEvents() {
    val visibilityChanged = NoArgEvent()
    val synapseRemoved = OneArgEvent<Synapse>()
    val synapseListChanged = NoArgEvent()
}