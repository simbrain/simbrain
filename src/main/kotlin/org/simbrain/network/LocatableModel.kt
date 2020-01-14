package org.simbrain.network

import org.simbrain.network.events.LocationEvents
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * Model elements that have a location should implement this interface. The location is a center location.
 * Some classes store their location (e.g [Neuron]) directly, while others determine their location based on objects
 * they contain (e.g. [NeuronGroup]).
 *
 * Kotlin note: java implementing classes must provide getters and setters for vars and getters for vals.
 */
interface LocatableModel : NetworkModel {

    /**
     * Center location of the [NetworkModel].
     */
    var location: Point2D

    /**
     * Implementing classes must fire and handle location events.
     */
    val events: LocationEvents

}

/**
 * Return top left location in a list of [LocatableModel] objects.
 */
val List<LocatableModel>.topLeftLocation
    get() = Point2D.Double(map { it.location.x }.min() ?: 0.0, map { it.location.y }.min() ?: 0.0)

/**
 * Return the center location in a list of [LocatableModel] objects.
 */
val List<LocatableModel>.centerLocation
    get() = Point2D.Double(bound.x + bound.width / 2, bound.y + bound.height / 2)

/**
 * Return bounding box for a list of [LocatableModel] objects.
 */
val List<LocatableModel>.bound: Rectangle2D
    get() {
        val minX = map { it.location.x }.min() ?: 0.0
        val minY = map { it.location.y }.min() ?: 0.0
        val maxX = map { it.location.x }.max() ?: 0.0
        val maxY = map { it.location.y }.max() ?: 0.0
        return Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY)
    }

