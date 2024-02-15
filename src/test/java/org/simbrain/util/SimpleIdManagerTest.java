package org.simbrain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleIdManagerTest {

    @Test
    public void testIdManager() {
        SimpleIdManager manager = new SimpleIdManager(c -> 4);
        assertEquals("String_4", manager.getAndIncrementId(String.class));
        assertEquals("String_5", manager.getAndIncrementId(String.class));
    }

    @Test
    public void testIncrements() {
        SimpleIdManager.SimpleId id = new SimpleIdManager.SimpleId("Base", 1);
        id.getAndIncrement();
        id.getAndIncrement();
        id.getAndIncrement();
        var name = id.getAndIncrement();
        assertEquals("Base_4", name);
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