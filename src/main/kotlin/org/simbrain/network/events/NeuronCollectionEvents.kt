package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * See [Events].
 */
class NeuronCollectionEvents: LocationEvents() {
    val customInfoUpdated = NoArgEvent()
    val fanInUpdated = NoArgEvent()
}