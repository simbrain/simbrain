package org.simbrain.util.projection

import org.simbrain.util.Events

class ProjectorEvents: Events() {
    val pointUpdated = OneArgEvent<DataPoint>()
    val datasetChanged = NoArgEvent()
    val datasetCleared = NoArgEvent()
    val settingsChanged = NoArgEvent()
    val methodChanged = ChangedEvent<ProjectionMethod>()
    val iterated = OneArgEvent<Double>()
    val startIterating = NoArgEvent()
    val stopIterating = NoArgEvent()
}
