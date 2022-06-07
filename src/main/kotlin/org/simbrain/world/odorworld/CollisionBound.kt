package org.simbrain.world.odorworld

import org.simbrain.util.distanceSqTo
import java.awt.geom.Line2D
import java.awt.geom.Point2D

abstract class CollisionBound {
    /**
     * Approximated collision bound.
     */
    abstract val collisionRadius: Double

    /**
     * Top left location of this collision bound.
     */
    abstract val location: Point2D

    /**
     * Center location of this collision bound.
     */
    abstract val centerLocation: Point2D

    /**
     * Actual collision bound.
     */
    protected abstract val collisionBounds: List<Line2D>

    /**
     * Velocity of this collision bound
     */
    var velocity = Point2D.Double(0.0, 0.0)

    /**
     * Check if this entity is colliding with other entity in a given direction.
     *
     * @param other the other entity
     * @return true if collided
     */
    fun collide(other: CollisionBound): List<Boolean> {
        if (isInCollisionRadius(other)) {
            return collisionBounds.map { a ->
                other.collisionBounds.any { b ->
                    a.intersectsLine(b)
                }
            }
        }
        return collisionBounds.map { false }
    }

    fun isInCollisionRadius(other: CollisionBound): Boolean {
        if (other === this) {
            return false
        }
        val distanceSq = location distanceSqTo other.location
        val totalRadius = collisionRadius + other.collisionRadius
        return totalRadius * totalRadius > distanceSq
    }

    fun setVelocity(dx: Double, dy: Double) {
        velocity.setLocation(dx, dy)
    }

}