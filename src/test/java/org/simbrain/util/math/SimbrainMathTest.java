package org.simbrain.util.math;

import org.junit.Test;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

public class SimbrainMathTest {

    @Test
    public void getRescaledValue() {
        assertEquals(5, SimbrainMath.rescale(0, -1, 1, 0, 10), 0.0);
        assertEquals(2.5, SimbrainMath.rescale(.25, 0, 1, 0, 10), 0.0);
        assertEquals(SimbrainMath.rescale(.25, 0, 1, -1, 1), -.5, 0.0);
        assertEquals(2, SimbrainMath.rescale(.5, 0, 1, 0, 4), 0.0);
        // Test clipping
        assertEquals(SimbrainMath.rescale(-1, 0, 1, -5, 4), -5, 0.0);
    }

    @Test
    public void clip() {
        assertEquals(0, (int) SimbrainMath.clip(-1, 0, 1));
        assertEquals(1, (int) SimbrainMath.clip(2, 0, 1));
    }

    @Test
    public void distance() {
        assertEquals(1, SimbrainMath.distance(new Point2D.Double(0, 0), new Point2D.Double(0, 1)), 0.0);
        assertEquals(1, SimbrainMath.distance(new Point2D.Double(0, 0), new Point2D.Double(1, 0)), 0.0);
        assertEquals(Math.sqrt(2), SimbrainMath.distance(new Point2D.Double(0, 0), new Point2D.Double(1, 1)), 0.0);
        assertEquals(Math.sqrt(8), SimbrainMath.distance(new Point2D.Double(-1, -1), new Point2D.Double(1, 1)), 0.0);

    }

    @Test
    public void log2() {
        assertEquals(2, SimbrainMath.log2(4),0.0);
        assertEquals(3, SimbrainMath.log2(8),0.0);
        assertEquals(1, SimbrainMath.log2(2),0.0);
        assertEquals(0, SimbrainMath.log2(1),0.0);

    }

    @Test
    public void multVector() {

        double[] baseVector = new double[]{0,1,2,3,-1,1.5};
        assertArrayEquals(baseVector, SimbrainMath.multVector(baseVector,1),0.0);
        double[] testVector1 = new double[]{0,2,4,6,-2,3};
        assertArrayEquals(testVector1, SimbrainMath.multVector(baseVector,2),0.0);
        double[] testVector2 = new double[]{0,-1,-2,-3,1,-1.5};
        assertArrayEquals(testVector2, SimbrainMath.multVector(baseVector,-1),0.0);
        double[] testVector3 = new double[]{0,.5,1,1.5,-.5,.75};
        assertArrayEquals(testVector3, SimbrainMath.multVector(baseVector,.5),0.0);
    }

    @Test
    public void addVector() {
        double[] baseVector = new double[]{0,1,2,3,-1,1.5};
        double[] baseVector2 = new double[]{0,.5,1,1.5,-.5,.75};

        assertArrayEquals(new double[]{0,2,4,6,-2,3}, SimbrainMath.addVector(baseVector,baseVector),0.0);
        assertArrayEquals(new double[]{0,1.5,3,4.5,-1.5,2.25}, SimbrainMath.addVector(baseVector,baseVector2),0.0);
    }

}