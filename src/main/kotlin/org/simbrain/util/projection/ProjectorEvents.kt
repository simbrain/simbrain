package org.simbrain.util.projection

import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class ProjectorEvents2: Events2() {
    val dataChanged = NoArgEvent()
    val pointFound = AddedEvent<DataPoint>()
    val colorsChanged = NoArgEvent()
    val methodChanged = ChangedEvent<ProjectionMethod>()
    val iteration = AddedEvent<Double>()

}

class ProjectorEvents3: Events2() {
    val pointAdded = AddedEvent<DoubleArray>()
    val datasetChanged = NoArgEvent()
    val settingsChanged = NoArgEvent()
    val methodChanged = ChangedEvent<ProjectionMethod2>()
    val iterated = AddedEvent<Double>()
    val startIterating = NoArgEvent()
    val stopIterating = NoArgEvent()

}