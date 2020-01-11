package org.simbrain.network

import org.simbrain.network.events.LocationEvents
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * Model elements that have a location should implement this interface.  Note that locations are mostly based on
 * Neuron location. Neurons have point locations but not width or height.  Thus the width  of a neuron group,
 * for example, is the distance between the point locations of the neurons within it which are farthest apart.
 */
interface LocatableModel : NetworkModel {

    /**
     * Center location of the [NetworkModel]
     */
    var location: Point2D

    val events: LocationEvents

}

val List<LocatableModel>.topLeftLocation
    get() = Point2D.Double(map { it.location.x }.min() ?: 0.0, map { it.location.y }.min() ?: 0.0)

val List<LocatableModel>.centerLocation
    get() = Point2D.Double(bound.x + bound.width / 2, bound.y + bound.height / 2)

val List<LocatableModel>.bound : Rectangle2D
    get() {
        val minX = map { it.location.x }.min() ?: 0.0
        val minY = map { it.location.y }.min() ?: 0.0
        val maxX = map { it.location.x }.max() ?: 0.0
        val maxY = map { it.location.y }.max() ?: 0.0
        return Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY)
    }

