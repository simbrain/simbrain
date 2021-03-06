package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

/**
 * @see Event
 */
open class LocationEvents(model: NetworkModel) : NetworkModelEvents(model) {

    /**
     * Handle location change events
     */
    open fun onLocationChange(handler: Runnable)  = "LocationChange".event(handler)

    /**
     * Fire a location change events.
     */
    open fun fireLocationChange() = "LocationChange"()

}