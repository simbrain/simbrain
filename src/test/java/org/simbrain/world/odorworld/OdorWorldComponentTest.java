package org.simbrain.world.odorworld;

import org.junit.jupiter.api.Test;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OdorWorldComponentTest {

    @Test
    public void testXStream() {

        // Create a world
        OdorWorldComponent oc = new OdorWorldComponent("Test");
        oc.getWorld().addEntity(new OdorWorldEntity(oc.getWorld(), EntityType.SWISS));

        // Create xstream for world
        String xstream = oc.getXML();
        //System.out.println(xstream);

        InputStream stream = new ByteArrayInputStream(xstream.getBytes(StandardCharsets.UTF_8));

        // Unmarshall from xstream
        OdorWorldComponent oc2 = OdorWorldComponent.open(stream, "test2", "xml");

        // Check a swiss cheese is there
        assertTrue(
                oc.getWorld().getEntityList().get(0).getEntityType() ==
                oc2.getWorld().getEntityList().get(0).getEntityType());


        // TODO: Test more features of the save
    }
}