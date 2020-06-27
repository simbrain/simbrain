package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

/**
 * @see Event
 */
abstract class LocationEvents(model: NetworkModel) : NetworkModelEvents(model) {

    /**
     * Handle location change events
     */
    abstract fun onLocationChange(handler: Runnable)

    /**
     * Fire a location change events.
     */
    abstract fun fireLocationChange()

}