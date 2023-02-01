package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.util.Events2
import java.awt.geom.Point2D

/**
 * See [Events2]
 */
class SynapseGroup2Events2: NetworkModelEvents2() {
    val visibilityChanged = NoArgEvent()
    val synapseAdded = AddedEvent<Synapse>()
    val synapseRemoved = RemovedEvent<Synapse>()
    val synapseListChanged = NoArgEvent()
    val locationChanged = ChangedEvent<Point2D>()
}