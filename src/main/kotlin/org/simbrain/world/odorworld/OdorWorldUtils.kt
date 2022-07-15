package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.BoundIntersection
import org.simbrain.world.odorworld.entities.Bounded
import kotlin.math.min


fun Bounded.intersect(other: Bounded): BoundIntersection {
    val a = this
    val topLeftA = a.topLeftLocation
    val b = other
    val topLeftB = b.topLeftLocation

    return if (b is OdorWorld) { // world bound is inverted
        val left = topLeftA.x - topLeftB.x
        val right = (topLeftB.x + b.width) - (topLeftA.x + a.width)
        val top = topLeftA.y - topLeftB.y
        val bottom = (topLeftB.y + b.height) - (topLeftA.y + a.height)
        val xCollision = -min(left, right)
        val yCollision = -min(top, bottom)
        BoundIntersection(xCollision > 0 || yCollision > 0, xCollision, yCollision)
    } else {
        val xCollision = min((topLeftA.x + a.width) - topLeftB.x, (topLeftB.x + b.width) - topLeftA.x)
        val yCollision = min((topLeftA.y + a.height) - topLeftB.y, (topLeftB.y + b.height) - topLeftA.y)

        BoundIntersection(xCollision > 0 && yCollision > 0, xCollision, yCollision)
    }

}

