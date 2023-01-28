package org.simbrain.network.events

import org.simbrain.network.core.NetworkTextObject
import org.simbrain.util.Event

/**
 * @see Event
 */
class NetworkTextEvents(val text: NetworkTextObject) : LocationEvents(text) {

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

}
