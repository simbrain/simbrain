package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class SynapseGroup2Events(val sg: SynapseGroup2) : NetworkModelEvents(sg) {

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onSynapseAdded(handler: Consumer<Synapse>) = "SynapseAdded".itemAddedEvent(handler)
    fun fireSynapseAdded(syn: Synapse) = "SynapseAdded"(new = syn)

    fun onSynapseRemoved(handler: Consumer<Synapse>) = "SynapseRemoved".itemRemovedEvent(handler)
    fun fireSynapseRemoved(syn: Synapse) = "SynapseRemoved"(old = syn)

    fun onSynapseListChanged(handler: Runnable) = "SynapseListChanged".event(handler)
    fun fireSynapseListChanged() = "SynapseListChanged"()

    fun onVisibilityChange(handler: Runnable) = "VisibilityChange".event(handler)
    fun fireVisibilityChange() = "VisibilityChange"()

}

// Lol
class SynapseGroup2Events2: NetworkModelEvents2() {
    val visibilityChanged = NoArgEvent()
    val synapseAdded = AddedEvent<Synapse>()
    val synapseRemoved = RemovedEvent<Synapse>()
    val synapseListChanged = NoArgEvent()
    val locationChanged = ChangedEvent<Point2D>()
}