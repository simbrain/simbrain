package org.simbrain.world.odorworld;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.util.piccolo.Tile;
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
    public void testOdorWorldSetTile() {
        world.getTileMap().addTile(5, 4, 4, true);
        assert(world.getTileMap().getTileStackAt(4, 4).stream().map(Tile::getId).anyMatch(id -> id == 5));
    }

    @Test
    public void testOdorWorldSetTileCollision() {
        world.getTileMap().addTile(5, 4, 4, true);
        assert(world.getTileMap().hasCollisionTile(4, 4));
    }

    @Test
    public void testOdorWorldSetTileNonCollision() {
        world.getTileMap().addTile(5, 4, 4, false);
        assert(!world.getTileMap().hasCollisionTile(4, 4));
    }

    @Test
    public void testUpdateMapSize() {
        world.getTileMap().updateMapSize(20, 10);
        assert(world.getTileMap().getMapWidthInTiles() == 20);
        assert(world.getTileMap().getMapHeightInTiles() == 10);
    }
}