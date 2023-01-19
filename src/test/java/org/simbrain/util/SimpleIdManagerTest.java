package org.simbrain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleIdManagerTest {

    @Test
    public void testIdManager() {
        SimpleIdManager manager = new SimpleIdManager(c -> 4);
        assertEquals(manager.getAndIncrementId(String.class), "String_4");
        assertEquals(manager.getAndIncrementId(String.class), "String_5");
        assertEquals(manager.getProposedId(String.class), "String_6");
    }

    @Test
    public void testIncrements() {
        SimpleIdManager.SimpleId id = new SimpleIdManager.SimpleId("Base", 1);
        id.getAndIncrement();
        id.getAndIncrement();
        id.getAndIncrement();
        assertEquals(4, id.getCurrentIndex());
    }

    @Test
    public void testProposedId() {
        SimpleIdManager.SimpleId id = new SimpleIdManager.SimpleId("Base", 1);
        id.getAndIncrement();
        id.getAndIncrement();
        id.getAndIncrement();
        assertEquals("Base_4", id.getProposedId());

        // The id should _not_ have been incremented by the last call
        assertEquals("Base_4", id.getAndIncrement());

        // The id should have been incremented by the last call
        assertEquals("Base_5", id.getAndIncrement());
    }


    @Test
    public void testCustomBaseName() {
        SimpleIdManager manager = new SimpleIdManager(c -> 4, c -> "test");
        assertEquals("test_4", manager.getAndIncrementId(String.class) );
    }


    @Test
    public void testDelimeter() {
        SimpleIdManager manager = new SimpleIdManager(c -> 1, c -> "test", "*");
        assertEquals("test*1", manager.getAndIncrementId(Object.class) );
    }


}