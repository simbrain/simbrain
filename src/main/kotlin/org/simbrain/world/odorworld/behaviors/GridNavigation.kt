/**
 * Cell-graph helpers for NPC behaviors running in [org.simbrain.world.odorworld.entities.MovementMode.GRID].
 * Steps are open when [OdorWorld.gridStepBlocker] finds no wall, edge or blocking tile; other entities are ignored
 * here and left to the step itself, which fires a collision when one is in the way.
 */
package org.simbrain.world.odorworld.behaviors

import org.simbrain.world.odorworld.GridDirection
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity

typealias GridCell = Pair<Int, Int>

/**
 * Directions in which a step from [cell] is not blocked by walls, the map edge or blocking tiles.
 */
fun OdorWorld.openGridDirections(cell: GridCell): List<GridDirection> =
    GridDirection.entries.filter { gridStepBlocker(cell.first, cell.second, it) == null }

/**
 * Breadth-first path lengths in steps from [start] to every cell, indexed as [column][row]. Unreachable cells,
 * including every cell when [start] is off the grid, hold [UNREACHABLE].
 */
fun OdorWorld.gridDistancesFrom(start: GridCell): Array<IntArray> {
    val distances = Array(gridColumns) { IntArray(gridRows) { UNREACHABLE } }
    if (!isCellOnGrid(start)) return distances
    val queue = ArrayDeque<GridCell>()
    distances[start.first][start.second] = 0
    queue.addLast(start)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        val next = distances[cell.first][cell.second] + 1
        for (direction in openGridDirections(cell)) {
            val neighbor = gridStepTarget(cell.first, cell.second, direction) ?: continue
            if (distances[neighbor.first][neighbor.second] != UNREACHABLE) continue
            distances[neighbor.first][neighbor.second] = next
            queue.addLast(neighbor)
        }
    }
    return distances
}

fun OdorWorld.isCellOnGrid(cell: GridCell) = cell.first in 0 until gridColumns && cell.second in 0 until gridRows

fun Array<IntArray>.distanceAt(cell: GridCell): Int = this[cell.first][cell.second]

/**
 * Queues [direction] as [entity]'s next grid step (or stops it when null) travelling at [speed] pixels per
 * update, zeroing the continuous movement channels so nothing else moves the entity this tick, and records
 * [notes] for the steering overlay.
 */
fun commitGridStep(entity: OdorWorldEntity, direction: GridDirection?, speed: Double, notes: String) {
    entity.pendingGridStep = direction
    if (direction != null && speed > 0) entity.gridSpeed = speed
    entity.movement.speed = 0.0
    entity.movement.dtheta = 0.0
    if (entity.showSteeringDebug) {
        entity.steeringDebug = SteeringDebugInfo(
            DoubleArray(0), DoubleArray(0), DoubleArray(0), direction?.heading ?: entity.heading, 0.0
        ).also { it.behaviorNotes = notes }
    }
}

const val UNREACHABLE = -1
