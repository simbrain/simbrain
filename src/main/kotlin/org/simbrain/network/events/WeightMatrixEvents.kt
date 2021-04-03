package org.simbrain.network.events

import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.util.function.BiConsumer

/**
 * @see Event
 */
class WeightMatrixEvents(val wm: WeightMatrix) : NetworkModelEvents(wm) {

    // Weight matrices are not LocatableModels but still we need to know about location changes
    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLineUpdated(handler: Runnable) = "LineUpdated".event(handler)
    fun fireLineUpdated() = "LineUpdated"()

}