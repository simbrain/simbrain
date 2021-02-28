package org.simbrain.util.projection

import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

/**
 * See [Event]
 */
class ProjectorEvents(projector: Projector) : Event(PropertyChangeSupport(projector)) {

    // TODO: These events were just used to retrofit what was there. Now that they are here, it is clear
    // that a revision is needed. There really only seems to be one use-case, of general update to the dataset.

    fun onProjectionMethodChanged(handler: Runnable) = "ProjectionMethodChanged".event(handler)
    fun fireProjectionMethodChanged() = "ProjectionMethodChanged"()

    // TODO: What is the right name for this? Check all uses.
    fun onDatasetInitialized(handler: Runnable) = "DatasetInitialized".event(handler)
    fun fireDatasetInitialized() = "DatasetInitialized"()

    // TODO: Consider passing point as argument
    fun onPointAdded(handler: Runnable) = "PointAdded".event(handler)
    fun firePointAdded() = "PointAdded"()

    fun onPointRemoved(handler: Runnable) = "PointRemoved".event(handler)
    fun firePointRemoved() = "PointRemoved"()

    fun onColorsChanged(handler: Runnable) = "ColorsChanged".event(handler)
    fun fireColorsChanged() = "ColorsChanged"()

}