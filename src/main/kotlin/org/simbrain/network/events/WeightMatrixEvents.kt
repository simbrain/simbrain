package org.simbrain.network.events

import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.util.function.BiConsumer

/**
 * @see Event
 */
class WeightMatrixEvents(val wm: WeightMatrix) : NetworkModelEvents(wm) {

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onLineUpdated(handler: Runnable) = "LineUpdated".event(handler)
    fun fireLineUpdated() = "LineUpdated"()


}