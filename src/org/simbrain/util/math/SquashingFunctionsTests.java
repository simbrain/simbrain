/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.jblas.DoubleMatrix;
import org.junit.Test;

/**
 * JUnit tests for static methods in squashing functions.
 *
 * @author Jeff Yoshimi
 */
public class SquashingFunctionsTests {

    @Test
    public void testSigmoids() {
        assertEquals(0, SquashingFunctions.atan(0, 1, -1, 1), 0.01);
        assertEquals(0, SquashingFunctions.logistic(0, 1, -1, 1), 0.01);
        assertEquals(0, SquashingFunctions.tanh(0, 1, -1, 1), 0.01);

        assertEquals(.5, SquashingFunctions.atan(0, 1, 0, 1), 0.01);
        assertEquals(.5, SquashingFunctions.logistic(0, 1, 0, 1), 0.01);
        assertEquals(.5, SquashingFunctions.tanh(0, 1, 0, 1), 0.01);

        // Below fails at tolerance of .01
        assertEquals(1, SquashingFunctions.atan(10, 1, -1, 1), 0.1);
        assertEquals(1, SquashingFunctions.logistic(10, 1, -1, 1), 0.01);
        assertEquals(1, SquashingFunctions.tanh(10, 1, -1, 1), 0.1);

        assertEquals(-1, SquashingFunctions.atan(-10, 1, -1, 1), 0.1);
        assertEquals(-1, SquashingFunctions.logistic(-10, 1, -1, 1), 0.1);
        assertEquals(-1, SquashingFunctions.tanh(-10, 1, -1, 1), 0.1);
    }

    @Test
    public void testDerivatives() {
        assertEquals(1, SquashingFunctions.derivAtan(0, 1, -1, 1), 0.01);
        assertEquals(1, SquashingFunctions.derivTanh(0, 1, -1, 1), 0.01);

        assertEquals(1, SquashingFunctions.derivLogistic(0, 1, -1, 1), 0.01);
        assertEquals(1, SquashingFunctions.derivLogistic(0, 1, 0, 1), 0.01);
        assertEquals(2, SquashingFunctions.derivLogistic(0, 1, -1, 2), 0.01);
        assertEquals(1, SquashingFunctions.derivLogistic(0, 1, 0, 1), 0.01);
        assertEquals(0, SquashingFunctions.derivLogistic(10, 1, -1, 1), 0.01);
        assertEquals(0, SquashingFunctions.derivLogistic(-10, 1, -1, 1), 0.01);
    }

    @Test
    public void testInverses() {
    }

    @Test
    public void testJBlasBackedFunctions() {

        // Objects for testing
        DoubleMatrix zeros = DoubleMatrix.zeros(5);
        DoubleMatrix halves = zeros.add(.5);
        DoubleMatrix ones = zeros.add(1);
        DoubleMatrix testOut = DoubleMatrix.zeros(5);

        // Test logistic and its derivative
        SquashingFunctions.logistic(zeros, testOut, 1, -1, 1);
        assertArrayEquals(zeros.data, testOut.data, .01);
        SquashingFunctions.logistic(zeros, testOut, 1, 0, 1);
        assertArrayEquals(halves.data, testOut.data, .01);

        SquashingFunctions.derivLogistic(zeros, testOut, 1, -1, 1);
        assertArrayEquals(ones.data, testOut.data, .01);

        // TODO: Test Tanh, arctan

    }

}
