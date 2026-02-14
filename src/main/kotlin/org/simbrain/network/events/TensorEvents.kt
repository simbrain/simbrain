package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * See [Events].
 */
class TensorEvents : LocationEvents() {
    val visualPropertiesChanged = NoArgEvent()
}
