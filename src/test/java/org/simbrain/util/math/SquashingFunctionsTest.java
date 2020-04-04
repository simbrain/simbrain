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

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.core.Network;
import org.simbrain.network.neuron_update_rules.LinearRule;

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
        INDArray inputs = Nd4j.linspace(-10, 10, 1000);
        inputs.putScalar(0, 0);
        inputs.putScalar(1, -1e9);
        inputs.putScalar(2, 1e9);

        INDArray actuals = Nd4j.zeros(1000);
        INDArray actualDerivs = Nd4j.zeros(1000);
        INDArray expecteds = Nd4j.zeros(1000);
        INDArray expectedDerivs = Nd4j.zeros(1000);

        SquashingFunctions.logistic(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivLogistic(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            assertEquals(actuals.getDouble(i), SquashingFunctions.logistic(inputs.getDouble(i), 1, 0, 1), epsilon);
            assertEquals(actualDerivs.getDouble(i), SquashingFunctions.derivLogistic(inputs.getDouble(i), 1, 0, 1), epsilon);
        }
        expecteds = actuals.dup();
        expectedDerivs = actualDerivs.dup();
        SquashingFunctions.logisticWithDerivative(inputs, actuals, actualDerivs, 1, 0, 1);

        assertArrayEquals(actuals.toDoubleVector(), expecteds.toDoubleVector(), epsilon);
        assertArrayEquals(actualDerivs.toDoubleVector(), expectedDerivs.toDoubleVector(), epsilon);

        SquashingFunctions.atan(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivAtan(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            double expected = SquashingFunctions.atan(inputs.getDouble(i), 1, 0, 1);
            double expectedDeriv = SquashingFunctions.derivAtan(inputs.getDouble(i), 1, 0, 1);
            assertEquals(actuals.getDouble(i), expected, epsilon);
            assertEquals(actualDerivs.getDouble(i), expectedDeriv, epsilon);
        }

        SquashingFunctions.tanh(inputs, actuals, 1, 0, 1);
        SquashingFunctions.derivTanh(inputs, actualDerivs, 1, 0, 1);
        for (int i = 0; i < 1000; ++i) {
            double expected = SquashingFunctions.tanh(inputs.getDouble(i), 1, 0, 1);
            double expectedDeriv = SquashingFunctions.derivTanh(inputs.getDouble(i), 1, 0, 1);
            assertEquals(actuals.getDouble(i), expected, epsilon);
            assertEquals(actualDerivs.getDouble(i), expectedDeriv, epsilon);
        }
    }

    // TODO: Remove scratch work below

    @Test
    public void quickTest() {
        INDArray in = Nd4j.ones(1);
        INDArray out = Nd4j.zeros(1);
        System.out.println(in);
        System.out.println(out);
        SquashingFunctions.atan(in, out, 1,0,1);
        System.out.println(in);
        System.out.println(out);
    }

    @Test
    public void test2() {
        INDArray in = Nd4j.ones(1);
        System.out.println(in);
        add2(in);
        System.out.println(in);
    }

     void add2(INDArray test) {
        test.addi(2);
    }
}
