package org.simbrain.util.projection

import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class ProjectorEvents(projector: Projector) : Event(PropertyChangeSupport(projector)) {

    // Data changed (either upstairs, downstairs, or both) and so the graphical points have to be re-rendered
    fun onDataChanged(handler: Runnable) = "DataChanged".event(handler)
    fun fireDataChanged() = "DataChanged"()

    // The indicated point was found in the dataset
    fun onPointFound(handler: Consumer<DataPoint>) = "PointFound".itemAddedEvent(handler)
    fun firePointFound(point: DataPoint) = "PointFound"(new = point)

    // The color scheme for the dataset was changed
    fun onColorsChanged(handler: Runnable) = "ColorsChanged".event(handler)
    fun fireColorsChanged() = "ColorsChanged"()

}