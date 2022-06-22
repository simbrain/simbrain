package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.BoundIntersection
import org.simbrain.world.odorworld.entities.Bounded
import kotlin.math.min


fun Bounded.intersect(other: Bounded): BoundIntersection {
    val a = this
    val b = other

    return if (b.worldBound) {
        val left = a.x - b.x
        val right = (b.x + b.width) - (a.x + a.width)
        val top = a.y - b.y
        val bottom = (b.y + b.height) - (a.y + a.height)
        val xCollision = -min(left, right)
        val yCollision = -min(top, bottom)
        BoundIntersection(xCollision > 0 || yCollision > 0, xCollision, yCollision)
    } else {
        val xCollision = min((a.x + a.width) - b.x, (b.x + b.width) - a.x)
        val yCollision = min((a.y + a.height) - b.y, (b.y + b.height) - a.y)

        BoundIntersection(xCollision > 0 && yCollision > 0, xCollision, yCollision)
    }

}

