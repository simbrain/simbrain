package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
class TensorEvents : LocationEvents() {
    val visualPropertiesChanged = NoArgEvent()
}
