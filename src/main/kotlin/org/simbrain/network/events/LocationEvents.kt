package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * See [Events]
 */
open class LocationEvents: NetworkModelEvents() {
    val locationChanged = NoArgEvent()
}