package org.simbrain.world.odorworld.entities;

import org.junit.Test;
import org.simbrain.world.odorworld.OdorWorld;

import static org.junit.Assert.*;

public class OdorWorldEntityTest {

    @Test
    public void testWorldBounds() {
        OdorWorld world = new OdorWorld();
        world.setWrapAround(false);
        OdorWorldEntity entity = new OdorWorldEntity(world);
        entity.setX(10);
        assertEquals(10.0, entity.getX(), 0.0);
        entity.setY(15);
        assertEquals(15.0, entity.getY(), 0.0);

        // These should not change the x and y values to the width and height
        entity.setX(world.getWidth() + 20);
        entity.setY(world.getHeight() + 20);
        assertEquals(world.getWidth(), entity.getX(), 0.0);
        assertEquals(world.getHeight(), entity.getY(), 0.0);

    }
}