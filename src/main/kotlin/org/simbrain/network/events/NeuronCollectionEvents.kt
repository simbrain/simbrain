package org.simbrain.network.events

import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see NetworkEvents
 */
class NeuronCollectionEvents(val nc: AbstractNeuronCollection) : Event(PropertyChangeSupport(nc)) {

    fun onDelete(handler: Consumer<AbstractNeuronCollection>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = nc)

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onRecordingStarted(handler: Runnable) = "RecordingStarted".event(handler)
    fun fireRecordingStarted() = "RecordingStarted"()

    fun onRecordingStopped(handler: Runnable) = "RecordingStopped".event(handler)
    fun fireRecordingStopped() = "RecordingStopped"()

    fun onNeuronAdded(handler: Consumer<Neuron>) = "NeuronAdded".itemAddedEvent(handler)
    fun fireNeuronAdded(neuron: Neuron) = "NeuronAdded"(new = neuron)


}