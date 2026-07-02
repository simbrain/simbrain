package org.simbrain.util.projection

import org.simbrain.util.FlowEvents

class ProjectorEvents: FlowEvents() {
    val pointUpdated = OneArgEvent<DataPoint>()
    val datasetChanged = NoArgEvent()
    val datasetCleared = NoArgEvent()
    val settingsChanged = NoArgEvent()
    val methodChanged = ChangedEvent<ProjectionMethod>()
    val iterated = AwaitableEvent<Double>()
    val startIterating = NoArgEvent()
    val stopIterating = NoArgEvent()
}
