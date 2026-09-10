/**
 * Maze walls in the 3D view: the cell walk that finds the first closed edge along a ray, and rendering checks that
 * walls occlude entities and textures vary along a column.
 */
package org.simbrain.world.odorworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.TileMap
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Color
import java.awt.image.BufferedImage

class RaycasterTest {

    private val cellSize = 64.0

    @Test
    fun `ray east from a cell center stops at the closed east wall`() {
        val maze = Maze(2, 1)
        val hit = Raycaster(4, 4).castMazeRay(maze, cellSize, 32.0, 32.0, 1.0, 0.0, 1000.0)
        assertNotNull(hit)
        assertEquals(32.0, hit!!.distance, 1e-9)
        assertEquals(0, hit.side)
        assertEquals(0.5, hit.u, 1e-9)
    }

    @Test
    fun `open edges are walked through to the next closed wall`() {
        val maze = Maze(2, 1).apply { setWall(0, 0, GridDirection.EAST, false) }
        val hit = Raycaster(4, 4).castMazeRay(maze, cellSize, 32.0, 32.0, 1.0, 0.0, 1000.0)
        assertNotNull(hit)
        assertEquals(96.0, hit!!.distance, 1e-9)
        assertEquals(0, hit.side)
    }

    @Test
    fun `diagonal ray reports the position along a horizontal wall`() {
        val maze = openMaze(3, 3)
        // Heads east and slightly north from the center of the top left cell, so the border above is hit first
        val hit = Raycaster(4, 4).castMazeRay(maze, cellSize, 32.0, 32.0, 1.0, -0.5, 1000.0)
        assertNotNull(hit)
        assertEquals(64.0, hit!!.distance, 1e-9)
        assertEquals(1, hit.side)
        assertEquals(0.5, hit.u, 1e-9)
    }

    @Test
    fun `rays starting outside the maze stop at its border`() {
        val maze = openMaze(2, 2)
        val hit = Raycaster(4, 4).castMazeRay(maze, cellSize, -40.0, 32.0, 1.0, 0.0, 1000.0)
        assertNotNull(hit)
        assertEquals(40.0, hit!!.distance, 1e-9)
        assertEquals(0, hit.side)
        assertEquals(0.5, hit.u, 1e-9)
    }

    @Test
    fun `walls beyond the view distance are not reported`() {
        val maze = Maze(2, 1)
        assertNull(Raycaster(4, 4).castMazeRay(maze, cellSize, 32.0, 32.0, 1.0, 0.0, 10.0))
    }

    @Test
    fun `axis aligned rays with a zero component hit the perpendicular wall`() {
        val maze = Maze(1, 2)
        val hit = Raycaster(4, 4).castMazeRay(maze, cellSize, 32.0, 32.0, 0.0, 1.0, 1000.0)
        assertNotNull(hit)
        assertEquals(32.0, hit!!.distance, 1e-9)
        assertEquals(1, hit.side)
    }

    private class Scene(val world: OdorWorld, val agent: OdorWorldEntity)

    /**
     * Three cells in a row: an agent in the west cell facing east and cheese in the middle cell.
     */
    private fun scene(maze: Maze): Scene {
        val world = OdorWorld().apply {
            tileMap = TileMap(6, 2)
            wrapAround = false
            gridCellSizeInTiles = 2
        }
        val agent = runBlocking {
            world.addEntity(96.0, 32.0, EntityType.Swiss)
            world.addEntity(32.0, 32.0, EntityType.Mouse).apply { heading = 0.0 }
        }
        world.maze = maze
        return Scene(world, agent)
    }

    private fun render(scene: Scene, textureWalls: Boolean): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        Raycaster(32, 32).render(
            world = scene.world,
            parentEntity = scene.agent,
            cameraX = 32.0,
            cameraY = 32.0,
            cameraHeading = 0.0,
            horizonPosition = 0.5,
            cameraWorldHeight = 16.0,
            fov = 90.0,
            viewDistance = 500.0,
            wallHeight = 2.0,
            billboardSprites = true,
            skyColor = Color(135, 206, 235),
            wallColor = Color(180, 140, 100),
            outputBuffer = image,
            textureWalls = textureWalls
        )
        return image
    }

    private fun BufferedImage.column(x: Int) = (0 until height).map { getRGB(x, it) }

    @Test
    fun `a closed maze wall fills the view and hides the entity behind it`() {
        val closed = render(scene(Maze(3, 1)), textureWalls = false)
        val open = render(scene(openMaze(3, 1)), textureWalls = false)

        val closedColumn = closed.column(16)
        assertEquals(1, closedColumn.distinct().size, "flat wall at half a cell should cover the whole column")
        val wall = Color(closedColumn.first())
        assertTrue(wall.red > wall.green && wall.green > wall.blue, "column should be the wall color, got $wall")

        val openColumn = open.column(16)
        assertTrue(openColumn.distinct().size > 1, "with the wall open the column shows sky, sprite and floor")
        assertNotEquals(closedColumn[20], openColumn[20])
    }

    @Test
    fun `textured walls vary along the column while flat walls do not`() {
        val flat = render(scene(Maze(3, 1)), textureWalls = false).column(16)
        val textured = render(scene(Maze(3, 1)), textureWalls = true).column(16)
        assertEquals(1, flat.distinct().size)
        assertTrue(textured.distinct().size > 1)
    }

    @Test
    fun `walls far from the camera but within view distance are still found`() {
        val maze = Maze(3, 3)
        val cell = 32.0
        val hit = Raycaster(4, 4).castMazeRay(maze, cell, 14.5 * cell, 14.5 * cell, -1.0, -1.0, 5000.0)
        assertNotNull(hit)
        assertEquals(11.5 * cell, hit!!.distance, 1e-9)
    }
}
