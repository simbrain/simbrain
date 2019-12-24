package org.simbrain.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleIdTest {

    @Test
    public void testIncrements() {
        SimpleId id = new SimpleId("Base", 1);
        id.getId();
        id.getId();
        id.getId();
        assertEquals(4, id.getCurrentIndex());
    }

    @Test
    public void testProposedId() {
        SimpleId id = new SimpleId("Base", 1);
        id.getId();
        id.getId();
        id.getId();
        assertEquals("Base_4", id.getProposedId());

        // The id should _not_ have been incremented by the last call
        assertEquals("Base_4", id.getId());

        // The id should have been incremented by the last call
        assertEquals("Base_5", id.getId());
    }


}