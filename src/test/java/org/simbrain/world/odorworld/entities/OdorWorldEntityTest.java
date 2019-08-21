package org.simbrain.world.odorworld.entities;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.world.odorworld.OdorWorld;

import static org.junit.Assert.*;

public class OdorWorldEntityTest {

    OdorWorld world;

    @Before
    public void setUp() throws Exception {
        world = new OdorWorld();
    }

    @Test
    public void testWorldBounds() {
        world.setWrapAround(false);
        OdorWorldEntity entity = new OdorWorldEntity(world);
        entity.setX(10);
        assertEquals(10.0, entity.getX(), 0.0);
        entity.setY(15);
        assertEquals(15.0, entity.getY(), 0.0);

        // These should not change the x and y values
        entity.setX(world.getWidth() + 20);
        entity.setY(world.getHeight() + 20);
        assertEquals(10, entity.getX(), 0.0);
        assertEquals(15, entity.getY(), 0.0);

    }

    @Test
    public void randomizeLocation() {
        OdorWorldEntity mouse = world.addAgent();
        for (int i = 0; i < 100; i++) {
            mouse.randomizeLocation();
            assert mouse.getLocation()[0] > 0;
            assert mouse.getLocation()[1] > 0;
            assert mouse.getLocation()[0] < world.getWidth();
            assert mouse.getLocation()[1] < world.getHeight();
        }
    }
}