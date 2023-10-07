package org.simbrain.util.projection

import org.simbrain.util.Events

class ProjectorEvents: Events() {
    val pointAdded = AddedEvent<DoubleArray>()
    val datasetChanged = NoArgEvent()
    val datasetCleared = NoArgEvent()
    val settingsChanged = NoArgEvent()
    val methodChanged = ChangedEvent<ProjectionMethod>()
    val iterated = AddedEvent<Double>()
    val startIterating = NoArgEvent()
    val stopIterating = NoArgEvent()
}
