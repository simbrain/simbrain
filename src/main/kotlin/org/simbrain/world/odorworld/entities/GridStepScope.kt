/**
 * Receiver for the block given to [OdorWorldEntity.queueGridSteps]. Each step call suspends until the entity has
 * either arrived in the next cell or been blocked, and returns which, so the block can branch on what it finds:
 * `while (east()) { }` walks to a wall, `if (!north()) east()` tries an alternative. The block is resumed by the
 * entity's own updates, on the world's thread; only the step calls in this scope should suspend inside it.
 */
package org.simbrain.world.odorworld.entities

import org.simbrain.world.odorworld.GridDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GridStepScope internal constructor(val entity: OdorWorldEntity, private val run: GridPlanRun) {

    /**
     * Step [count] cells in [direction], one per cell center. Returns false as soon as a step is blocked, leaving
     * the entity where it stopped.
     */
    suspend fun step(direction: GridDirection, count: Int = 1): Boolean {
        repeat(count) {
            val arrived = suspendCoroutine { continuation ->
                run.pending = PendingGridStep(direction, continuation)
            }
            if (!arrived) return false
        }
        return true
    }

    suspend fun north(count: Int = 1) = step(GridDirection.NORTH, count)

    suspend fun south(count: Int = 1) = step(GridDirection.SOUTH, count)

    suspend fun east(count: Int = 1) = step(GridDirection.EAST, count)

    suspend fun west(count: Int = 1) = step(GridDirection.WEST, count)

    /**
     * Whether a step in [direction] from the current cell is open, without taking it.
     */
    fun canStep(direction: GridDirection): Boolean {
        val (column, row) = entity.cell
        return entity.world.gridStepBlocker(column, row, direction, entity) == null
    }
}

internal class PendingGridStep(val direction: GridDirection, val continuation: Continuation<Boolean>)

/**
 * One running plan: the step its block is waiting on, the continuation to resume once a begun step arrives, and
 * the result handed back to whoever queued it.
 */
internal class GridPlanRun {
    val completion = kotlinx.coroutines.CompletableDeferred<Boolean>()
    var pending: PendingGridStep? = null
    var awaitingArrival: Continuation<Boolean>? = null
}
