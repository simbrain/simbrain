/**
 * Maze walls for [OdorWorld]: a grid of cells whose shared edges may be closed. Walls have no thickness and sit
 * on cell boundaries, so they never occupy tiles. The world owns cell sizing; this file only knows cell indices
 * and converts to pixels when asked.
 */
package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.Bound
import org.simbrain.world.odorworld.entities.Bounded
import kotlin.random.Random

/**
 * Cardinal grid directions with the [OdorWorld] heading convention (0 is east, counterclockwise positive, y grows
 * downward on screen).
 */
enum class GridDirection(val dx: Int, val dy: Int, val heading: Double) {
    EAST(1, 0, 0.0),
    NORTH(0, -1, 90.0),
    WEST(-1, 0, 180.0),
    SOUTH(0, 1, 270.0);

    val opposite: GridDirection
        get() = when (this) {
            EAST -> WEST
            NORTH -> SOUTH
            WEST -> EAST
            SOUTH -> NORTH
        }

    companion object {
        /**
         * The direction whose heading is closest to [heading].
         */
        fun fromHeading(heading: Double): GridDirection {
            val normalized = ((heading % 360.0) + 360.0) % 360.0
            return when (((normalized + 45.0) / 90.0).toInt() % 4) {
                0 -> EAST
                1 -> NORTH
                2 -> WEST
                else -> SOUTH
            }
        }
    }
}

/**
 * A single zero-thickness wall in pixel space. [x1], [y1] to [x2], [y2] is axis aligned.
 */
data class WallSegment(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
    fun toBound(): Bounded = Bound((x1 + x2) / 2, (y1 + y2) / 2, x2 - x1, y2 - y1)
}

class Maze(val columns: Int, val rows: Int) {

    init {
        require(columns > 0 && rows > 0) { "Maze needs at least one cell" }
    }

    /**
     * verticalWalls[c][r] is the wall on the west side of cell (c, r). Index c == columns is the east border.
     */
    private val verticalWalls = Array(columns + 1) { BooleanArray(rows) { true } }

    /**
     * horizontalWalls[c][r] is the wall on the north side of cell (c, r). Index r == rows is the south border.
     */
    private val horizontalWalls = Array(columns) { BooleanArray(rows + 1) { true } }

    fun isInside(column: Int, row: Int) = column in 0 until columns && row in 0 until rows

    fun hasWall(column: Int, row: Int, direction: GridDirection): Boolean {
        require(isInside(column, row)) { "Cell ($column, $row) is outside the maze" }
        return when (direction) {
            GridDirection.WEST -> verticalWalls[column][row]
            GridDirection.EAST -> verticalWalls[column + 1][row]
            GridDirection.NORTH -> horizontalWalls[column][row]
            GridDirection.SOUTH -> horizontalWalls[column][row + 1]
        }
    }

    fun setWall(column: Int, row: Int, direction: GridDirection, present: Boolean) {
        require(isInside(column, row)) { "Cell ($column, $row) is outside the maze" }
        when (direction) {
            GridDirection.WEST -> verticalWalls[column][row] = present
            GridDirection.EAST -> verticalWalls[column + 1][row] = present
            GridDirection.NORTH -> horizontalWalls[column][row] = present
            GridDirection.SOUTH -> horizontalWalls[column][row + 1] = present
        }
        segmentCache = null
    }

    /**
     * Whether the edge leaving ([column], [row]) in [direction] is walled, seen from whichever side of the edge
     * lies inside the maze. Works for cells outside the maze too, so a step or ray from outside stops at the
     * border just as one from inside does.
     */
    fun hasEdgeWall(column: Int, row: Int, direction: GridDirection): Boolean {
        if (isInside(column, row)) return hasWall(column, row, direction)
        val nextColumn = column + direction.dx
        val nextRow = row + direction.dy
        return isInside(nextColumn, nextRow) && hasWall(nextColumn, nextRow, direction.opposite)
    }

    /**
     * The edge leaving ([column], [row]) in [direction] as a collision bound, whether or not it is walled.
     */
    fun edgeWall(column: Int, row: Int, direction: GridDirection, cellSize: Double): Bounded {
        val x = column * cellSize
        val y = row * cellSize
        val segment = when (direction) {
            GridDirection.WEST -> WallSegment(x, y, x, y + cellSize)
            GridDirection.EAST -> WallSegment(x + cellSize, y, x + cellSize, y + cellSize)
            GridDirection.NORTH -> WallSegment(x, y, x + cellSize, y)
            GridDirection.SOUTH -> WallSegment(x, y + cellSize, x + cellSize, y + cellSize)
        }
        return segment.toBound()
    }

    /**
     * True when an agent in cell ([column], [row]) can step one cell in [direction] without crossing a wall.
     * Stepping off the maze is never open, since the border is always walled.
     */
    fun isOpen(column: Int, row: Int, direction: GridDirection): Boolean {
        if (!isInside(column, row)) return false
        if (!isInside(column + direction.dx, row + direction.dy)) return false
        return !hasWall(column, row, direction)
    }

    @Transient
    private var segmentCache: SegmentCache? = null

    private class SegmentCache(val cellSize: Double, val segments: List<WallSegment>, val bounds: List<Bounded>)

    private fun cacheFor(cellSize: Double): SegmentCache {
        segmentCache?.takeIf { it.cellSize == cellSize }?.let { return it }
        val segments = buildSegments(cellSize)
        return SegmentCache(cellSize, segments, segments.map { it.toBound() }).also { segmentCache = it }
    }

    /**
     * Every present wall as a pixel-space segment, given the pixel size of one cell. The list is cached until a
     * wall changes, so callers may compare it by identity to detect changes.
     */
    fun wallSegments(cellSize: Double): List<WallSegment> = cacheFor(cellSize).segments

    private fun buildSegments(cellSize: Double): List<WallSegment> = buildList {
        for (c in 0..columns) {
            for (r in 0 until rows) {
                if (verticalWalls[c][r]) {
                    add(WallSegment(c * cellSize, r * cellSize, c * cellSize, (r + 1) * cellSize))
                }
            }
        }
        for (c in 0 until columns) {
            for (r in 0..rows) {
                if (horizontalWalls[c][r]) {
                    add(WallSegment(c * cellSize, r * cellSize, (c + 1) * cellSize, r * cellSize))
                }
            }
        }
    }

    fun collisionBounds(cellSize: Double): List<Bounded> = cacheFor(cellSize).bounds

    companion object {

        /**
         * A perfect maze (a spanning tree over the cells, so there is exactly one path between any two cells)
         * generated by recursive backtracking. The same [seed] always yields the same maze.
         */
        fun recursiveBacktracker(columns: Int, rows: Int, seed: Long? = null): Maze {
            val random = if (seed == null) Random.Default else Random(seed)
            val maze = Maze(columns, rows)
            val visited = Array(columns) { BooleanArray(rows) }
            val stack = ArrayDeque<Pair<Int, Int>>()
            val start = random.nextInt(columns) to random.nextInt(rows)
            visited[start.first][start.second] = true
            stack.addLast(start)
            val directions = GridDirection.entries.toMutableList()
            while (stack.isNotEmpty()) {
                val (c, r) = stack.last()
                directions.shuffle(random)
                val next = directions.firstOrNull { d ->
                    val nc = c + d.dx
                    val nr = r + d.dy
                    maze.isInside(nc, nr) && !visited[nc][nr]
                }
                if (next == null) {
                    stack.removeLast()
                    continue
                }
                maze.setWall(c, r, next, false)
                val nc = c + next.dx
                val nr = r + next.dy
                visited[nc][nr] = true
                stack.addLast(nc to nr)
            }
            return maze
        }
    }
}
