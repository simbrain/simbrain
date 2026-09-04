package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.point
import org.simbrain.world.odorworld.entities.Bound
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MazeTest {

    @Test
    fun `new maze has every wall closed including the border`() {
        val maze = Maze(3, 2)
        // 4 vertical wall columns of 2 plus 3 horizontal wall rows of 3
        assertEquals(4 * 2 + 3 * 3, maze.wallCount)
        assertFalse(maze.isOpen(0, 0, GridDirection.EAST))
        assertFalse(maze.isOpen(0, 0, GridDirection.WEST))
    }

    @Test
    fun `opening a wall is visible from both cells`() {
        val maze = Maze(2, 2)
        maze.setWall(0, 0, GridDirection.EAST, false)
        assertTrue(maze.isOpen(0, 0, GridDirection.EAST))
        assertTrue(maze.isOpen(1, 0, GridDirection.WEST))
        assertFalse(maze.isOpen(0, 0, GridDirection.SOUTH))
    }

    @Test
    fun `stepping off the edge is never open`() {
        val maze = Maze.openGrid(2, 2)
        assertFalse(maze.isOpen(0, 0, GridDirection.WEST))
        assertFalse(maze.isOpen(1, 1, GridDirection.SOUTH))
        assertTrue(maze.isOpen(0, 0, GridDirection.EAST))
    }

    @Test
    fun `recursive backtracker produces a spanning tree over all cells`() {
        val columns = 7
        val rows = 5
        val maze = Maze.recursiveBacktracker(columns, rows, seed = 42L)
        assertEquals(columns * rows, maze.reachableCellCount())
        val interiorWalls = (columns - 1) * rows + columns * (rows - 1)
        val borderWalls = 2 * columns + 2 * rows
        val openInteriorWalls = interiorWalls + borderWalls - maze.wallCount
        assertEquals(columns * rows - 1, openInteriorWalls)
    }

    @Test
    fun `same seed yields the same maze`() {
        val a = Maze.recursiveBacktracker(6, 6, seed = 7L)
        val b = Maze.recursiveBacktracker(6, 6, seed = 7L)
        assertEquals(a.wallSegments(64.0), b.wallSegments(64.0))
    }

    @Test
    fun `wall segments and collision bounds line up with cell edges`() {
        val maze = Maze(1, 1)
        val segments = maze.wallSegments(64.0)
        assertEquals(4, segments.size)
        assertTrue(segments.contains(WallSegment(64.0, 0.0, 64.0, 64.0)))
        assertTrue(segments.contains(WallSegment(0.0, 64.0, 64.0, 64.0)))
        val bounds = maze.collisionBounds(64.0)
        assertTrue(bounds.any { it.width == 0.0 && it.height == 64.0 && it.x == 64.0 })
    }

    @Test
    fun `blocking wall is returned only for closed edges`() {
        val maze = Maze(2, 1)
        assertNotNull(maze.blockingWall(0, 0, GridDirection.EAST, 64.0))
        maze.setWall(0, 0, GridDirection.EAST, false)
        assertNull(maze.blockingWall(0, 0, GridDirection.EAST, 64.0))
    }

    @Test
    fun `zero width wall blocks a box that straddles it`() {
        val wall = WallSegment(64.0, 0.0, 64.0, 64.0).toBound()
        val straddling = Bound(60.0, 32.0, 40.0, 40.0)
        val clear = Bound(30.0, 32.0, 40.0, 40.0)
        assertTrue(straddling.intersect(wall).intersect)
        assertFalse(clear.intersect(wall).intersect)
    }

    @Test
    fun `direction from heading rounds to the nearest quarter turn`() {
        assertEquals(GridDirection.EAST, GridDirection.fromHeading(20.0))
        assertEquals(GridDirection.NORTH, GridDirection.fromHeading(100.0))
        assertEquals(GridDirection.WEST, GridDirection.fromHeading(-170.0))
        assertEquals(GridDirection.SOUTH, GridDirection.fromHeading(300.0))
        assertEquals(point(1, 0), point(GridDirection.EAST.dx, GridDirection.EAST.dy))
    }

    @Test
    fun `maze survives an xstream round trip of the world`() {
        val component = OdorWorldComponent("Maze")
        component.world.generateMaze(4, 3, cellSizeInTiles = 2, seed = 99L)
        val expected = component.world.maze!!.wallSegments(64.0)
        val bytes = ByteArrayOutputStream().also { component.save(it, null) }.toByteArray()
        val restored = OdorWorldComponent.open(ByteArrayInputStream(bytes), "Maze", null)
        assertEquals(2, restored.world.gridCellSizeInTiles)
        assertEquals(expected, restored.world.maze!!.wallSegments(64.0))
        assertEquals(12, restored.world.maze!!.reachableCellCount())
    }
}
