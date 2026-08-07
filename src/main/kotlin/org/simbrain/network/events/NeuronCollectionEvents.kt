package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
class NeuronCollectionEvents: LocationEvents() {
    val customInfoUpdated = NoArgEvent()
    val fanInUpdated = NoArgEvent()
    val shouldUpdateOutline = NoArgEvent(interval = 10, timingMode = TimingMode.Throttle)

    /**
     * The collection's label array changed, because a member neuron was relabeled or membership changed.
     */
    val labelArrayChanged = NoArgEvent()
}