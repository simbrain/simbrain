package org.simbrain.network.events

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * @see NetworkEvents
 */
class NetworkTextEvents(val text: NetworkTextObject) : Event(PropertyChangeSupport(text)), LocationEvents {

    fun onDelete(handler: Consumer<NetworkTextObject>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = text)

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

}