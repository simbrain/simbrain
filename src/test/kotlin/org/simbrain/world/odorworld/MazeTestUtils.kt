package org.simbrain.world.odorworld

/**
 * A maze whose interior walls are all open, leaving only the border.
 */
fun openMaze(columns: Int, rows: Int) = Maze(columns, rows).apply {
    for (c in 0 until columns) {
        for (r in 0 until rows) {
            if (c + 1 < columns) setWall(c, r, GridDirection.EAST, false)
            if (r + 1 < rows) setWall(c, r, GridDirection.SOUTH, false)
        }
    }
}

val Maze.wallCount: Int
    get() = wallSegments(1.0).size

/**
 * Cells reachable from (0, 0), for checking that a maze is fully connected.
 */
fun Maze.reachableCellCount(): Int {
    val visited = Array(columns) { BooleanArray(rows) }
    val stack = ArrayDeque(listOf(0 to 0))
    visited[0][0] = true
    var count = 0
    while (stack.isNotEmpty()) {
        val (c, r) = stack.removeLast()
        count++
        for (direction in GridDirection.entries) {
            if (!isOpen(c, r, direction)) continue
            val nc = c + direction.dx
            val nr = r + direction.dy
            if (!visited[nc][nr]) {
                visited[nc][nr] = true
                stack.addLast(nc to nr)
            }
        }
    }
    return count
}
