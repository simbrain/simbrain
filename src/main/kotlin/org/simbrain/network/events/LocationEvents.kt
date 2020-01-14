package org.simbrain.network.events

/**
 * Handle location events.
 */
interface LocationEvents {

    /**
     * Handle location change events
     */
    fun onLocationChange(handler: Runnable)

    /**
     * Fire a location change events.
     */
    fun fireLocationChange()

}