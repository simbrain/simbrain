package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.*

class TileMapTest {

    var world = OdorWorld()

    @Test
    fun testEditingExistingTile() {
        world.tileMap.setTile("Tile Layer 1", 4, 4, 25)
        assertEquals(25, world.tileMap.layers[0][4, 4])
    }

    @Test
    fun `test loading tilemap`() {
        world.tileMap = loadTileMap("yulins_world.tmx")
    }

    @Test
    fun `test filling a layer`() {
        world.tileMap.fill("Grass1")
        assertEquals("Grass1", world.tileMap.getTileStackAt(0,0).first().label)
    }

    @Test
    fun testUpdateMapSize() {
        world.tileMap.updateMapSize(20, 10)
        assert(world.tileMap.width == 20)
        assert(world.tileMap.height == 10)
    }

    @Test
    fun `test pixel coordinates to grid coordinates`() {
        world.tileMap = loadTileMap("empty.tmx")
        with(world.tileMap) {
            // Tile Width is 32
            val coord = PixelCoordinate(32+16, 32+32+16)
            val gridCoord = coord.toGridCoordinate().int
            assertEquals(1, gridCoord.x)
            assertEquals(2, gridCoord.y)
        }
    }

    @Test
    fun `test grid coordinates to pixel coordinates`() {
        world.tileMap = loadTileMap("empty.tmx")
        with(world.tileMap) {
            val gridCoord = GridCoordinate(1,2)
            // Should return center of that pixel
            val pixelCoordinate = gridCoord.toPixelCoordinate().int
            assertEquals(32 + 32/2, pixelCoordinate.x)
            assertEquals(2*32 + 32/2, pixelCoordinate.y)
        }
    }

    @Test
    fun `test grid coordinates in radius`() {
        world.tileMap = loadTileMap("empty.tmx")
        var gridLocations = world.tileMap.getRelativeGridLocationsInRadius(15.0).toList()
        assertEquals(1, gridLocations.size)
        assertEquals(0.0, gridLocations.first().x)
        assertEquals(0.0, gridLocations.first().y)

        // Corner tiles are sqrt(32*32+32*32) = 46 pixels away
        gridLocations = world.tileMap.getRelativeGridLocationsInRadius(47.0).toList()
        assertEquals(9, gridLocations.size)

    }

}