package org.simbrain.network.events

import org.simbrain.network.groups.Subnetwork
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see NetworkEvents
 */
class SubnetworkEvents(val subnet: Subnetwork) : Event(PropertyChangeSupport(subnet)) {

    fun onDelete(handler: Consumer<Subnetwork>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = subnet)

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()


}