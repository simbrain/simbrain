package org.simbrain.network.events

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * @see Event
 */
class NetworkTextEvents(val text: NetworkTextObject) : NetworkModelEvents(text), LocationEvents {

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

}