package org.simbrain.network.events

import org.simbrain.network.groups.Subnetwork
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class SubnetworkEvents(val subnet: Subnetwork) : Event(PropertyChangeSupport(subnet)), LocationEvents {

    fun onDelete(handler: Consumer<Subnetwork>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = subnet)

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()


}