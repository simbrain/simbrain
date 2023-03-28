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
    val methodChanged2 = ChangedEvent<ProjectionMethod2>()
    val iteration = AddedEvent<Double>()

}