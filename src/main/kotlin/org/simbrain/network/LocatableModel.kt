package org.simbrain.network

import org.simbrain.network.events.LocationEvents
import org.simbrain.util.*
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
    get() = Point2D.Double(map { it.location.x }.minOrNull() ?: 0.0, map { it.location.y }.minOrNull() ?: 0.0)

/**
 * Return the center location in a list of [LocatableModel] objects.
 */
val List<LocatableModel>.centerLocation
    get() = Point2D.Double(bound.x + bound.width / 2, bound.y + bound.height / 2)

val List<LocatableModel>.minX get() = map { it.location.x }.minOrNull() ?: 0.0
val List<LocatableModel>.minY get() = map { it.location.y }.minOrNull() ?: 0.0
val List<LocatableModel>.maxX get() = map { it.location.x }.maxOrNull() ?: 0.0
val List<LocatableModel>.maxY get() = map { it.location.y }.maxOrNull() ?: 0.0

/**
 * Top-left and bottom-right locations padded with a small delta to make sure the bound has some size
 */
val List<LocatableModel>.minMax
    get() = point(minX, minY) - point(0.001, 0.001) to point(maxX, maxY) + point(0.001, 0.001)

/**
 * The four vertices of the bound in the order of topLeft, topRight, bottomLeft, bottomRight
 */
val List<LocatableModel>.vertices: RectangleVertices
    get() {
        val (min, max) = minMax
        return RectangleVertices(
                point(min.x, min.y),
                point(max.x, min.y),
                point(min.x, max.y),
                point(max.x, max.y)
        )
    }

/**
 * The four sides of the bound.
 */
val List<LocatableModel>.outlines get() = vertices.outlines

/**
 * Return bounding box for a list of [LocatableModel] objects.
 */
val List<LocatableModel>.bound: Rectangle2D
    get() {
        val (min, max) = minMax
        return rectangle(min, max)
    }

fun List<LocatableModel>.translate(vector: Point2D) = forEach { it.location = it.location + vector }
fun List<LocatableModel>.translate(dx: Double, dy: Double) = translate(point(dx, dy))

