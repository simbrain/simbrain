package org.simbrain.world.odorworld;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import static org.junit.Assert.*;

// Another example of a simple test.  To be updated and improved.  Still in initial experimentation with unit tests...
public class OdorWorldTest {

    OdorWorld world;

    @Before
    public void setUp() {
        world = new OdorWorld();
    }

    @Test
    public void testOdorWorldDefaultCreation() {
        assertNotNull(world.getTileMap());
    }

    @Test
    public void testOdorWorldSetEntityCenterLocation() {
        OdorWorldEntity entity = new OdorWorldEntity(world);
        entity.setCenterLocation(10,20);
        assert(entity.getCenterLocation()[0] == 10);
        assert(entity.getCenterLocation()[1] == 20);
    }

    @Test
    public void testEditingExistingTile() {
        world.getTileMap().editTile("Tile Layer 1", 25, 4, 4);
        assertEquals(25, world.getTileMap().getLayers().get(0).getTileIdAt(4, 4).intValue());
    }

    @Test
    public void testUpdateMapSize() {
        world.getTileMap().updateMapSize(20, 10);
        assert(world.getTileMap().getMapWidthInTiles() == 20);
        assert(world.getTileMap().getMapHeightInTiles() == 10);
    }

    @Test
    public void testSetCollisionProperty() {
        world.getTileMap().getLayer("Tile Layer 1").setProperty("collide", "true");
        assertTrue(world.getTileMap().getLayer("Tile Layer 1").isCollideLayer());
    }
}