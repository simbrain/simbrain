package org.simbrain.util.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void testNextDoubleSeed() {
        SimbrainRandomizer sr1 = new SimbrainRandomizer(5);
        double firstVal = sr1.nextDouble(-1,1);
        for (int i = 0; i < 100; i++) {
            double val = sr1.rand.nextDouble(-5.0,5.0);
            assertTrue(val <= 5.0);
            assertTrue(val >= -5.0);
        }
        SimbrainRandomizer sr2 = new SimbrainRandomizer(5);
        double secondVal = sr2.nextDouble(-1,1);
        assertTrue(firstVal == secondVal);
    }

    @Test
    public void testNextIntSeed() {
        SimbrainRandomizer sr1 = new SimbrainRandomizer(5);
        int firstVal = sr1.nextInteger(-1,1);
        for (int i = 0; i < 100; i++) {
            int val = sr1.nextInteger(-5,5);
            assertTrue(val <= 5);
            assertTrue(val >= -5);
        }
        SimbrainRandomizer sr2 = new SimbrainRandomizer(5);
        int secondVal = sr2.nextInteger(-1,1);
        assertTrue(firstVal == secondVal);
    }
}