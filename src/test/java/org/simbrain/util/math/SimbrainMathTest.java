package org.simbrain.util.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimbrainMathTest {

    @Test
    public void getRescaledValue() {
        assertTrue(SimbrainMath.rescale(0,-1,1,0,10) == 5);
        assertTrue(SimbrainMath.rescale(.25,0,1,0,10) == 2.5);
        assertTrue(SimbrainMath.rescale(.25,0,1,-1,1) == -.5);
        assertTrue(SimbrainMath.rescale(.5,0,1,0,4) == 2);
        // Test clipping
        assertTrue(SimbrainMath.rescale(-1,0,1,-5,4) == -5);
    }

    @Test
    public void clip() {
        assertTrue(SimbrainMath.clip(-1, 0, 1) == 0);
        assertTrue(SimbrainMath.clip(2, 0, 1) == 1);
    }
}