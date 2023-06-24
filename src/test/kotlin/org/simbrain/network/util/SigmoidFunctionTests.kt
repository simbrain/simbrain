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
package org.simbrain.network.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.linspace
import org.simbrain.util.math.SigmoidFunctions
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix

class SigmoidFunctionTests {

    // TODO: Matrix derivative tests
    // TODO: Failing inverse tanh.  Seems to be a problem in the formula itself.

    val epsilon = 0.001

    /**
     * Input array where first three entries test special cases
     */
    val inputMatrix = linspace(-10.0, 10.0, 100).also {
        it[0] = 0.0 // Zero
        it[1] = -1e9 // Negative number with large absolute value -> lower bound of sigmoid
        it[2] = 1e9 // Large number -> Upper bound of sigmoid
    }.toMatrix()


    @Test
    fun `test scalar atan`() {
        assertEquals(0.0, SigmoidFunctions.atan(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(1.0, SigmoidFunctions.atan(1000.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(-1.0, SigmoidFunctions.atan(-1000.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(.5, SigmoidFunctions.atan(0.0, 1.0, 0.0, 1.0), epsilon)
    }

    @Test
    fun `test scalar logistic`() {
        assertEquals(0.0, SigmoidFunctions.logistic(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(.5, SigmoidFunctions.logistic(0.0, 1.0, 0.0, 1.0), epsilon)
        assertEquals(-1.0, SigmoidFunctions.logistic(-1000.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(1.0, SigmoidFunctions.logistic(1000.0, 1.0, -1.0, 1.0), epsilon)
    }

    @Test
    fun `test scalar tanh`() {
        assertEquals(0.0, SigmoidFunctions.tanh(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(1.0, SigmoidFunctions.tanh(1000.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(.5, SigmoidFunctions.tanh(0.0, 1.0, 0.0, 1.0), epsilon)
        assertEquals(-1.0, SigmoidFunctions.tanh(-1000.0, 1.0, -1.0, 1.0), epsilon)
    }

    @Test
    fun `test matrix logistic `() {
        val outputMatrix = SigmoidFunctions.logistic(inputMatrix, 1.0, 0.0, 1.0);
        // print(outputMatrix.toDoubleArray().contentToString())
        assertArrayEquals(inputMatrix.toDoubleArray().map { SigmoidFunctions.logistic(it, 1.0, 0.0, 1.0) }
            .toDoubleArray(),
            outputMatrix.toDoubleArray())
    }

    @Test
    fun `test matrix atan `() {
        val outputMatrix = SigmoidFunctions.atan(inputMatrix, 1.0, 0.0, 1.0);
        assertArrayEquals(inputMatrix.toDoubleArray().map { SigmoidFunctions.atan(it, 1.0, 0.0, 1.0) }.toDoubleArray(),
            outputMatrix.toDoubleArray())
    }

    @Test
    fun `test matrix tanh `() {
        val outputMatrix = SigmoidFunctions.tanh(inputMatrix, 1.0, 0.0, 1.0);
        assertArrayEquals(inputMatrix.toDoubleArray().map { SigmoidFunctions.tanh(it, 1.0, 0.0, 1.0) }.toDoubleArray(),
            outputMatrix.toDoubleArray())
    }
    @Test
    fun `test logistic derivative`() {
        assertEquals(1.0, SigmoidFunctions.derivLogistic(0.0, 1.0, -1.0, 1.0), 0.01)
        assertEquals(1.0, SigmoidFunctions.derivLogistic(0.0, 1.0, 0.0, 1.0), 0.01)
        assertEquals(2.0, SigmoidFunctions.derivLogistic(0.0, 1.0, -1.0, 2.0), 0.01)
        assertEquals(1.0, SigmoidFunctions.derivLogistic(0.0, 1.0, 0.0, 1.0), 0.01)
        assertEquals(0.0, SigmoidFunctions.derivLogistic(10.0, 1.0, -1.0, 1.0), 0.01)
        assertEquals(0.0, SigmoidFunctions.derivLogistic(-10.0, 1.0, -1.0, 1.0), 0.01)
    }

    @Test
    fun `test atan derivative`() {
        assertEquals(1.0, SigmoidFunctions.derivAtan(0.0, 1.0, -1.0, 1.0), 0.01)
    }

    @Test
    fun `test tanh derivative`() {
        assertEquals(1.0, SigmoidFunctions.derivTanh(0.0, 1.0, -1.0, 1.0), 0.01)
    }

    @Test
    fun `test scalar inverse atan`() {
        assertEquals(0.0, SigmoidFunctions.invAtan(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(0.0, SigmoidFunctions.invAtan(0.5, 1.0, 0.0, 1.0), epsilon)
        assertTrue(10 < SigmoidFunctions.invAtan(0.999, 1.0, -1.0, 1.0))
        assertTrue(-10 > SigmoidFunctions.invAtan(-0.999, 1.0, -1.0, 1.0))
    }

    @Test
    fun `test scalar inverse tanh`() {
        assertEquals(0.0, SigmoidFunctions.invTanh(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(0.0, SigmoidFunctions.invTanh(0.5, 1.0, 0.0, 1.0), epsilon)
        // assertTrue(10 < SigmoidFunctions.invTanh(0.999, 1.0, -1.0, 1.0));
        // assertTrue(-10 > SigmoidFunctions.invTanh(-0.999, 1.0, -1.0, 1.0));
    }

    @Test
    fun `test scalar inverse logistic`() {
        assertEquals(0.0, SigmoidFunctions.invLogistic(0.0, 1.0, -1.0, 1.0), epsilon)
        assertEquals(0.0, SigmoidFunctions.invLogistic(0.5, 1.0, 0.0, 1.0), epsilon)
        assertTrue(10 < SigmoidFunctions.invLogistic(0.999, 1.0, -1.0, 1.0))
        assertTrue(-10 > SigmoidFunctions.invLogistic(-0.999, 1.0, -1.0, 1.0))
    }

    // Matrix derivative tests old code
    //     // Logistic derivative
    //     INDArray expectedOutputs = Nd4j.zeros(100);
    //     INDArray expectedDerivs = Nd4j.zeros(100);
    //     expectedOutputs = outputs.dup();
    //     expectedDerivs = derivs.dup();
    //     SquashingFunctions.logisticWithDerivative(inputs, outputs, derivs, 1, 0, 1);
    //     assertArrayEquals(expectedOutputs.toDoubleVector(), outputs.toDoubleVector(), epsilon);
    //     assertArrayEquals(expectedDerivs.toDoubleVector(), derivs.toDoubleVector(), epsilon);
    //
    //     // Atan
    //     SquashingFunctions.atan(inputs, outputs, 1, 0, 1);
    //     SquashingFunctions.derivAtan(inputs, derivs, 1, 0, 1);
    //     for (int i = 0; i < 100; ++i) {
    //         double expected = SquashingFunctions.atan(inputs.getDouble(i), 1, 0, 1);
    //         double expectedDeriv = SquashingFunctions.derivAtan(inputs.getDouble(i), 1, 0, 1);
    //         assertEquals(expected, outputs.getDouble(i), epsilon);
    //         assertEquals(expectedDeriv, derivs.getDouble(i), epsilon);
    //     }
    //
    //     // Tanh
    //     SquashingFunctions.tanh(inputs, outputs, 1, 0, 1);
    //     SquashingFunctions.derivTanh(inputs, derivs, 1, 0, 1);
    //     for (int i = 0; i < 100; ++i) {
    //         double expected = SquashingFunctions.tanh(inputs.getDouble(i), 1, 0, 1);
    //         double expectedDeriv = SquashingFunctions.derivTanh(inputs.getDouble(i), 1, 0, 1);
    //         assertEquals(expected, outputs.getDouble(i), epsilon);
    //         assertEquals(expectedDeriv, derivs.getDouble(i), epsilon);
    //     }

}