package org.simbrain.network.events

import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class NeuronCollectionEvents(val nc: AbstractNeuronCollection) : Event(PropertyChangeSupport(nc)), LocationEvents {

    fun onDelete(handler: Consumer<AbstractNeuronCollection>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = nc)

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String?, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onRecordingStarted(handler: Runnable) = "RecordingStarted".event(handler)
    fun fireRecordingStarted() = "RecordingStarted"()

    fun onRecordingStopped(handler: Runnable) = "RecordingStopped".event(handler)
    fun fireRecordingStopped() = "RecordingStopped"()

    fun onNeuronAdded(handler: Consumer<Neuron>) = "NeuronAdded".itemAddedEvent(handler)
    fun fireNeuronAdded(neuron: Neuron) = "NeuronAdded"(new = neuron)


}