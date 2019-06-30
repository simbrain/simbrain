package org.simbrain.util.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimbrainRandomizerTest {

    @Test
    public void testNextDouble() {
        for (int i = 0; i < 100; i++) {
            double val = SimbrainRandomizer.rand.nextDouble(-5.0,5.0);
            assertTrue(val <= 5.0);
            assertTrue(val >= -5.0);
        }
    }

    @Test
    public void testNextInt() {
        for (int i = 0; i < 100; i++) {
            int val = SimbrainRandomizer.rand.nextInteger(-5,5);
            assertTrue(val <= 5);
            assertTrue(val >= -5);
        }
    }
}