package org.simbrain.network.events

import org.simbrain.network.dl4j.WeightMatrix
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class WeightMatrixEvents(val wm: WeightMatrix) : NetworkModelEvents(wm) {

    fun onDelete(handler: Consumer<WeightMatrix>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = wm)

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onLineUpdated(handler: Runnable) = "LineUpdated".event(handler)
    fun fireLineUpdated() = "LineUpdated"()


}