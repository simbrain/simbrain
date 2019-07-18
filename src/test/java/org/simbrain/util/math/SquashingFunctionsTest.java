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

import org.jblas.DoubleMatrix;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit tests for static methods in {@link SquashingFunctions}.
 *
 * @author Jeff Yoshimi
 */
public class SquashingFunctionsTest {

    @Test
    public void testSigmoids() {
        double epsilon = 0.001;
        assertEquals(0, SquashingFunctions.atan(0, 1, -1, 1), epsilon);
        assertEquals(0, SquashingFunctions.logistic(0, 1, -1, 1), epsilon);
        assertEquals(0, SquashingFunctions.tanh(0, 1, -1, 1), epsilon);

        assertEquals(.5, SquashingFunctions.atan(0, 1, 0, 1), epsilon);
        assertEquals(.5, SquashingFunctions.logistic(0, 1, 0, 1), epsilon);
        assertEquals(.5, SquashingFunctions.tanh(0, 1, 0, 1), epsilon);

        assertEquals(1, SquashingFunctions.atan(1000, 1, -1, 1), epsilon);
        assertEquals(1, SquashingFunctions.logistic(1000, 1, -1, 1), epsilon);
        assertEquals(1, SquashingFunctions.tanh(1000, 1, -1, 1), epsilon);

        assertEquals(-1, SquashingFunctions.atan(-1000, 1, -1, 1), epsilon);
        assertEquals(-1, SquashingFunctions.logistic(-1000, 1, -1, 1), epsilon);
        assertEquals(-1, SquashingFunctions.tanh(-1000, 1, -1, 1), epsilon);
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
        // TODO: Inverse tanh doesn't seem to work.
        double epsilon = 0.001;
        assertEquals(0, SquashingFunctions.invAtan(0, 1, -1, 1), epsilon);
        assertEquals(0, SquashingFunctions.invLogistic(0, 1, -1, 1), epsilon);
        assertEquals(0, SquashingFunctions.invTanh(0, 1, -1, 1), epsilon);

        assertEquals(0, SquashingFunctions.invAtan(0.5, 1, 0, 1), epsilon);
        assertEquals(0, SquashingFunctions.invLogistic(0.5, 1, 0, 1), epsilon);
        assertEquals(0, SquashingFunctions.invTanh(0.5, 1, 0, 1), epsilon);

        assertTrue(10 < SquashingFunctions.invAtan(0.999, 1, -1, 1));
        assertTrue(10 < SquashingFunctions.invLogistic(0.999, 1, -1, 1));
        //assertTrue(10 < SquashingFunctions.invTanh(0.999, 1, -1, 1));

        assertTrue(-10 > SquashingFunctions.invAtan(-0.999, 1, -1, 1));
        assertTrue(-10 > SquashingFunctions.invLogistic(-0.999, 1, -1, 1));
        //assertTrue(-10 > SquashingFunctions.invTanh(-0.999, 1, -1, 1));
    }

    @Test
    public void testMatrixFunctions() {

        // Check that the matrix forms are approximately equal to the singular forms
        double epsilon = 0.001;
        DoubleMatrix inputs = DoubleMatrix.linspace(-10, 10, 1000);
        inputs.put(0, 0);
        inputs.put(1, -1e9);
        inputs.put(2, 1e9);

        DoubleMatrix actuals = DoubleMatrix.zeros(1000);
        DoubleMatrix actualDerivs = DoubleMatrix.zeros(1000);
        DoubleMatrix expecteds = DoubleMatrix.zeros(1000);
        DoubleMatrix expectedDerivs = DoubleMatrix.zeros(1000);

        SquashingFunctions.logistic(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivLogistic(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            assertEquals(actuals.get(i), SquashingFunctions.logistic(inputs.get(i), 1, 0, 1), epsilon);
            assertEquals(actualDerivs.get(i), SquashingFunctions.derivLogistic(inputs.get(i), 1, 0, 1), epsilon);
        }
        expecteds.copy(actuals);
        expectedDerivs.copy(actualDerivs);
        SquashingFunctions.logisticWithDerivative(inputs, actuals, actualDerivs, 1, 0, 1);
        assertArrayEquals(actuals.data, expecteds.data, epsilon);
        assertArrayEquals(actualDerivs.data, expectedDerivs.data, epsilon);

        SquashingFunctions.atan(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivAtan(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            double expected = SquashingFunctions.atan(inputs.get(i), 1, 0, 1);
            double expectedDeriv = SquashingFunctions.derivAtan(inputs.get(i), 1, 0, 1);
            assertEquals(actuals.get(i), expected, epsilon);
            assertEquals(actualDerivs.get(i), expectedDeriv, epsilon);
        }

        SquashingFunctions.tanh(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivTanh(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            double expected = SquashingFunctions.tanh(inputs.get(i), 1, 0, 1);
            double expectedDeriv = SquashingFunctions.derivTanh(inputs.get(i), 1, 0, 1);
            assertEquals(actuals.get(i), expected, epsilon);
            assertEquals(actualDerivs.get(i), expectedDeriv, epsilon);
        }
    }
}
