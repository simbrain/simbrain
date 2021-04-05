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
class NeuronCollectionEvents(val nc: AbstractNeuronCollection) : LocationEvents(nc) {

    fun onRecordingStarted(handler: Runnable) = "RecordingStarted".event(handler)
    fun fireRecordingStarted() = "RecordingStarted"()

    fun onRecordingStopped(handler: Runnable) = "RecordingStopped".event(handler)
    fun fireRecordingStopped() = "RecordingStopped"()

}