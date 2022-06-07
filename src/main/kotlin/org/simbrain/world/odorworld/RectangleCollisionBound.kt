package org.simbrain.world.odorworld

import org.simbrain.util.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface Rectangular {
    var x: Double
    var y: Double
    var width: Double
    var height: Double
}


class RectangleCollisionBound(val shape: Rectangle2D) {

    var velocity: Point2D = point(0, 0)

    /**
     * the collision radius is approximately the circle enclosing the rectangular bound of this object.
     * 1.5 is about sqrt(2), which is the ratio of length of the diagonal line to the side of a square,
     * and the radius is half of that.
     * the velocity is also taken into account in case of high speed object.
     */
    val collisionRadius: Double
        get() = max(
            shape.width + abs(velocity.x),
            shape.height + abs(velocity.y)
        ) * 0.75

    var location: Point2D
        get() = shape.topLeft
        set(value) {
            shape.setTopLeftLocation(value)
        }

    var centerLocation: Point2D
        get() = shape.center
        set(value) {
            shape.centerOn(value)
        }

    fun veolcityWithoutCollidingWith(other: RectangleCollisionBound): CollisionResult {
        val collisionResult = getIntersectionTime(this, other, velocity)
        val otherCollisionResult = getIntersectionTime(other, this, -velocity)

        val willCollide = collisionResult != null && otherCollisionResult != null

        val t1 = collisionResult?.time ?: 1.0
        val t2 = 1.0 - (otherCollisionResult?.time ?: 0.0)

        val t = min(t1, t2)

        return CollisionResult(velocity * t, willCollide)
    }

    fun setLocation(x: Double, y: Double) {
        shape.setTopLeftLocation(x, y)
    }

    fun setSize(w: Double, h: Double) {
        shape.setSize(w, h)
    }


    fun setVelocity(dx: Double, dy: Double) {
        velocity.setLocation(dx, dy)
    }
}

fun getIntersectionTime(a: RectangleCollisionBound, b: RectangleCollisionBound, velocity: Point2D): Intersection.Time? {
    return a.shape.vertices.toList().map { it.withVector(velocity) }
        .flatMap { v -> b.shape.outlines.toList().map { side -> v.intersectionTime(side) } }
        .filterIsInstance<Intersection.Time>()
        .minByOrNull { it.time }
}

data class CollisionResult(val maxVelocity: Point2D, @get:JvmName("willCollide") val willCollide: Boolean)