package org.simbrain.network.events

import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class NeuronCollectionEvents2: LocationEvents2() {
    val recordingStarted = NoArgEvent()
    val recordingStopped = NoArgEvent()
}