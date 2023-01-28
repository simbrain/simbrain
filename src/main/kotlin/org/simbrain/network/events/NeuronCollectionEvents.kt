package org.simbrain.network.events

import org.simbrain.network.groups.AbstractNeuronCollection
import org.simbrain.util.Event

/**
 * @see Event
 */
class NeuronCollectionEvents(val nc: AbstractNeuronCollection) : LocationEvents(nc) {

    fun onRecordingStarted(handler: Runnable) = "RecordingStarted".event(handler)
    fun fireRecordingStarted() = "RecordingStarted"()

    fun onRecordingStopped(handler: Runnable) = "RecordingStopped".event(handler)
    fun fireRecordingStopped() = "RecordingStopped"()

}

class NeuronCollectionEvents2: LocationEvents2() {
    val recordingStarted = NoArgEvent()
    val recordingStopped = NoArgEvent()
}