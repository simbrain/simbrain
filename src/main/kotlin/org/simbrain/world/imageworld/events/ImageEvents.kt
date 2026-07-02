package org.simbrain.world.imageworld.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
class ImageEvents: FlowEvents() {
    val imageUpdate = NoArgAwaitableEvent()
    val resize = NoArgAwaitableEvent()
}