package org.simbrain.network.events

import org.simbrain.network.dl4j.NeuronArray
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class NeuronArrayEvents(val neuronArray: NeuronArray) : Event(PropertyChangeSupport(neuronArray)), LocationEvents {

    fun onDelete(handler: Consumer<NeuronArray>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = neuronArray)

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()


}