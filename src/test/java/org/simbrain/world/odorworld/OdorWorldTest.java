package org.simbrain.world.odorworld;

import org.junit.Test;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import static org.junit.Assert.*;

// Another example of a simple test.  To be updated and improved.  Still in initial experimentation with unit tests...
public class OdorWorldTest {

    @Test
    public void testOdorWorld() {
        OdorWorld world = new OdorWorld();
        OdorWorldEntity entity = new OdorWorldEntity(world);
        entity.setCenterLocation(10,20);
        assert(entity.getCenterLocation()[0] == 10);
        assert(entity.getCenterLocation()[1] == 20);
    }
}