package org.simbrain.world.odorworld

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.piccolo.loadTileMap

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

}