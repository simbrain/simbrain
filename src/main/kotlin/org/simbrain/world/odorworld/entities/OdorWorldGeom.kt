@file:JvmName("OdorWorldGeom")
package org.simbrain.world.odorworld.entities

import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.world.odorworld.events.EntityLocationEvent
import java.awt.geom.Point2D


interface StaticallyLocatable {
    val x: Double
    val y: Double
    val location: Point2D
}

interface Locatable : StaticallyLocatable {
    override var x: Double
    override var y: Double
    override var location: Point2D
}

/**
 * Top-left x, y position of the entity.
 */
class Location(@Transient private val event: EntityLocationEvent) : Locatable {
    @Transient
    private var dirty = true

    @UserParameter(label = "X", description = "X Position", order = 3)
    override var x = 0.0
        set(value) {
            field = value
            event.moved.fire()
            dirty = true
        }

    @UserParameter(label = "Y", description = "Y Position", order = 3)
    override var y = 0.0
        set(value) {
            field = value
            event.moved.fire()
            dirty = true
        }

    @Transient
    override var location: Point2D = point(x, y)
        get() {
            if (dirty) {
                field = point(x, y)
            }
            dirty = false
            return field
        }
        set(value) {
            field = value
            x = value.x
            y = value.y
        }
}

/**
 * Heading in degrees with 0 facing right.
 */
interface Rotatable {
    var heading: Double
}

class Rotation(@Transient private val event: EntityLocationEvent) : Rotatable {
    @UserParameter(label = "heading", description = "heading", order = 2)
    override var heading = 0.0
        set(value) {
            field = ((value % 360.0) + 360.0) % 360.0
            event.moved.fire()
        }
}

/**
 * Common interface for [Size] and [Bounded].
 */
interface WithSize {
    val width: Double
    val height: Double
    val size: Point2D
        get() = point(width, height)
}

class Size(override val width: Double, override val height: Double) : WithSize

interface Bounded : StaticallyLocatable, WithSize {
    val topLeftLocation: Point2D
        get() = point(x - width / 2, y - height / 2)

    /**
     * given a point return a point bounded in these bounds. if bounds are 100,100 and 150,150 is given it returns 100,100
     */
    fun bounded(point: Point2D) = point(point.x.coerceIn(x, width), point.y.coerceIn(y, height))

    /**
     * given a point return a point wrapped around these bounds. if bounds are 100,100 and 150,150 is given it returns 50,50
     */
    fun wrapAround(point: Point2D) = point(point.x % width, point.y % height)

    fun contains(point: Point2D) = topLeftLocation.let { tl ->
        point.x in tl.x..tl.x + width && point.y in tl.y..tl.y + height
    }

    fun isInXBounds(point: Point2D) = topLeftLocation.let { tl ->
        point.x in tl.x..tl.x + width
    }

    fun isInYBounds(point: Point2D) = topLeftLocation.let { tl ->
        point.y in tl.y..tl.y + height
    }

}

class Bound(
    override val x: Double,
    override val y: Double,
    override val width: Double,
    override val height: Double
) : Bounded {
    override val location: Point2D = point(x, y)
}

data class BoundIntersection(val intersect: Boolean, val dx: Double, val dy: Double)

interface Movable {
    var speed: Double
    var dtheta: Double
}

class Movement : Movable {
    override var speed: Double = 0.0

    override var dtheta: Double = 0.0

    override fun toString(): String {
        return "Movement(speed=$speed, dtheta=$dtheta)"
    }

}

interface ManuallyMovable : Movable {
    /**
     * Amount to manually move forward or in cardinal directions.
     */
    @get:UserParameter(label = "Straight movement", order = 10)
    var manualStraightMovementIncrement: Double

    /**
     * Amount to manually rotate.
     */
    @get:UserParameter(label = "Turn amount", order = 10)
    var manualMotionTurnIncrement: Double

    fun increaseSpeed()
    fun decreaseSpeed()
    fun turnLeft()
    fun turnRight()
    fun stopTurning()
}

class ManualMovement(
    override var manualStraightMovementIncrement: Double = 1.0,
    override var manualMotionTurnIncrement: Double = 1.0
) : ManuallyMovable, Movable by Movement() {

    override fun increaseSpeed() {
        speed += manualStraightMovementIncrement
    }

    override fun decreaseSpeed() {
        speed -= manualStraightMovementIncrement
    }

    override fun turnLeft() {
        dtheta = manualMotionTurnIncrement
    }

    override fun turnRight() {
        dtheta = -manualMotionTurnIncrement
    }

    override fun stopTurning() {
        dtheta = 0.0
    }

    override fun toString(): String {
        return "Movement(speed=$speed, dtheta=$dtheta)"
    }
}