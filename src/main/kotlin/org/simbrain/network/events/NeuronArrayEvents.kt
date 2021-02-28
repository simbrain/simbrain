package org.simbrain.network.events

import org.simbrain.network.matrix.NeuronArray
import org.simbrain.util.Event
import java.util.function.BiConsumer

/**
 * @see Event
 */
class NeuronArrayEvents(val neuronArray: NeuronArray) : LocationEvents(neuronArray) {

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()


}