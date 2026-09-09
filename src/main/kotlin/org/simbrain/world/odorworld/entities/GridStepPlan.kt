/**
 * Builder for a sequence of grid steps handed to [OdorWorldEntity.queueGridSteps]. Plain Kotlin control flow
 * works inside the builder, so `repeat` and conditionals compose with the direction helpers.
 */
package org.simbrain.world.odorworld.entities

import org.simbrain.world.odorworld.GridDirection

class GridStepPlan {

    internal val steps = ArrayList<QueuedGridStep>()

    /**
     * Step [count] cells in [direction]. A blocked step ends the plan.
     */
    fun step(direction: GridDirection, count: Int = 1) {
        repeat(count) { steps += QueuedGridStep(direction, untilBlocked = false) }
    }

    fun north(count: Int = 1) = step(GridDirection.NORTH, count)

    fun south(count: Int = 1) = step(GridDirection.SOUTH, count)

    fun east(count: Int = 1) = step(GridDirection.EAST, count)

    fun west(count: Int = 1) = step(GridDirection.WEST, count)

    /**
     * Keep stepping in [direction] until a wall, the map edge or a blocking object stops it, then continue with
     * the rest of the plan. Being stopped here is expected and does not end the plan.
     */
    fun untilBlocked(direction: GridDirection) {
        steps += QueuedGridStep(direction, untilBlocked = true)
    }
}

internal class QueuedGridStep(val direction: GridDirection, val untilBlocked: Boolean)
