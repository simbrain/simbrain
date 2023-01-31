package org.simbrain.network.events

import org.simbrain.util.Events2

/**
 * See [Events2]
 */
open class LocationEvents2: NetworkModelEvents2() {
    val locationChanged = NoArgEvent()
}