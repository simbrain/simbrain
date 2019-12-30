package org.simbrain.network.events

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see NetworkEvents
 */
class NetworkTextEvents(val text: NetworkTextObject) : Event(PropertyChangeSupport(text)) {

    fun onDelete(handler: Consumer<NetworkTextObject>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = text)
    
    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

}