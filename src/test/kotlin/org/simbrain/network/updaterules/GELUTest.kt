package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix

class GELUTest {

    val net = Network()

    var input = Neuron()
    var outputRule = GELU()
    val output = Neuron(outputRule)
    var weight = Synapse(input, output)

    //for a 1-1 network
    var input5x5 = NeuronArray(5)
    var output5x5 = NeuronArray(5)
    var weightMatrix5x5 = WeightMatrix(input5x5, output5x5)

    //for a few to many network
    var input3x5 = NeuronArray(3)
    var output3x5 = NeuronArray(5)
    var weightMatrix3x5 = WeightMatrix(input3x5, output3x5)

    //for a many to few network
    var input5x3 = NeuronArray(5)
    var output5x3 = NeuronArray(3)
    var weightMatrix5x3 = WeightMatrix(input5x3, output5x3)

    init {
        net.addNetworkModelsAsync(input, output, weight)
        input.activation = 1.0
        input.clamped = true

        //add 1-1 network
        net.addNetworkModelsAsync(input5x5, output5x5, weightMatrix5x5)
        output5x5.updateRule = GELU()

        //add few-many network
        weightMatrix3x5.setWeights(doubleArrayOf(1.0, 0.5, 0.5,
            0.5, 1.0, 0.5,
            0.5, 0.5, 1.0,
            0.5, 1.0, 0.5,
            1.0, 0.5, 0.5,))
        net.addNetworkModelsAsync(input3x5, output3x5, weightMatrix3x5)
        output3x5.updateRule = GELU()

        //add many-few network
        weightMatrix5x3.setWeights(doubleArrayOf(1.0, 0.5, 0.5, 0.5, 1.0,
            0.5, 1.0, 0.5, 1.0, 0.5,
            0.5, 0.5, 1.0, 0.5, 0.5))
        net.addNetworkModelsAsync(input5x3, output5x3, weightMatrix5x3)
        output5x3.updateRule = GELU()

    }

    //All expected values calculated using Desmos
    @Test
    fun `test scalar update`() {
        //Testing activation values for the linear portions as well as the curve

        val delta = 1e-5
        input.activation = -10.0
        net.update()
        assertEquals(0.0, output.activation, delta)

        input.activation = -6.0
        net.update()
        assertEquals(-0.0000000001, output.activation, delta)

        input.activation = -5.5
        net.update()
        assertEquals(-0.0000000059, output.activation, delta)

        input.activation = -5.0
        net.update()
        assertEquals(-0.0000002292, output.activation, delta)

        input.activation = -4.5
        net.update()
        assertEquals(-0.0000051367, output.activation, delta)

        input.activation = -4.0
        net.update()
        assertEquals(-0.0000702459, output.activation, delta)

        input.activation = -3.5
        net.update()
        assertEquals(-0.000616198, output.activation, delta)

        input.activation = -3.0
        net.update()
        assertEquals(-0.00363739, output.activation, delta)

        input.activation = -2.5
        net.update()
        assertEquals(-0.01508427, output.activation, delta)

        input.activation = -2.0
        net.update()
        assertEquals(-0.0454023, output.activation, delta)

        input.activation = -1.5
        net.update()
        assertEquals(-0.1004284, output.activation, delta)

        input.activation = -1.0
        net.update()
        assertEquals(-0.158808, output.activation, delta)

        input.activation = -0.752461
        net.update()
        assertEquals(-0.1700408, output.activation, delta)

        input.activation = -0.5
        net.update()
        assertEquals(-0.154286, output.activation, delta)

        input.activation = 0.0
        net.update()
        assertEquals(0.0, output.activation, 0.0)

        input.activation = 0.5
        net.update()
        assertEquals(0.345714, output.activation, delta)

        input.activation = 1.0
        net.update()
        assertEquals(0.841192, output.activation, delta)

        input.activation = 1.5
        net.update()
        assertEquals(1.399572, output.activation, delta)

        input.activation = 2.0
        net.update()
        assertEquals(1.954598, output.activation, delta)

        input.activation = 2.5
        net.update()
        assertEquals(2.484916, output.activation, delta)

        input.activation = 3.0
        net.update()
        assertEquals(2.996363, output.activation, delta)

        input.activation = 3.5
        net.update()
        assertEquals(3.49938, output.activation, delta)

        input.activation = 4.0
        net.update()
        assertEquals(3.99993, output.activation, delta)

        input.activation = 4.5
        net.update()
        assertEquals(4.49999, output.activation, delta)

        input.activation = 5.0
        net.update()
        assertEquals(5.0, output.activation, delta)

        input.activation = 10.0
        net.update()
        assertEquals(10.0, output.activation, delta)

        //Testing activation values for randomly generated points up to the thousandths place
        input.activation = -548.899
        net.update()
        assertEquals(0.0, output.activation, delta)

        input.activation = 742.876
        net.update()
        assertEquals(742.876, output.activation, delta)

        input.activation = -54.991
        net.update()
        assertEquals(0.0, output.activation, delta)

        input.activation = -271.202
        net.update()
        assertEquals(0.0, output.activation, delta)

        input.activation = 63.619
        net.update()
        assertEquals(63.619, output.activation, delta)
    }


    @Test
    fun `test scalar derivative`() {
        //Testing derivative values for the linear portions as well as the s-curve

        val delta = 1e-6
        assertEquals(-0.0, outputRule.getDerivative(-100.0), delta)
        assertEquals(-0.0, outputRule.getDerivative(-10.0), delta)
        assertEquals(-0.0000000008, outputRule.getDerivative(-6.0), delta)
        assertEquals(-0.0000015464, outputRule.getDerivative(-5.0), delta)
        assertEquals(-0.0000293223, outputRule.getDerivative(-4.5), delta)
        assertEquals(-0.000335123, outputRule.getDerivative(-4.0), delta)
        assertEquals(-0.002422644, outputRule.getDerivative(-3.5), delta)
        assertEquals(-0.01158417, outputRule.getDerivative(-3.0), delta)
        assertEquals(-0.0379516, outputRule.getDerivative(-2.5), delta)
        assertEquals(-0.0860993, outputRule.getDerivative(-2.0), delta)
        assertEquals(-0.1277108, outputRule.getDerivative(-1.5), delta)
        assertEquals(-0.0829641, outputRule.getDerivative(-1.0), delta)
        assertEquals(0.0, outputRule.getDerivative(-0.752461), delta)
        assertEquals(0.1326301, outputRule.getDerivative(-0.5), delta)
        assertEquals(0.5, outputRule.getDerivative(0.0), delta)
        assertEquals(0.86737, outputRule.getDerivative(0.5), delta)
        assertEquals(1.082964, outputRule.getDerivative(1.0), delta)
        assertEquals(1.127711, outputRule.getDerivative(1.5), delta)
        assertEquals(1.086099, outputRule.getDerivative(2.0), delta)
        assertEquals(1.037952, outputRule.getDerivative(2.5), delta)
        assertEquals(1.011584, outputRule.getDerivative(3.0), delta)
        assertEquals(1.002423, outputRule.getDerivative(3.5), delta)
        assertEquals(1.000335, outputRule.getDerivative(4.0), delta)
        assertEquals(1.000029, outputRule.getDerivative(4.5), delta)
        assertEquals(1.000002, outputRule.getDerivative(5.0), delta)
        assertEquals(1.0, outputRule.getDerivative(6.0), delta)
        assertEquals(1.0, outputRule.getDerivative(10.0), delta)
        assertEquals(1.0, outputRule.getDerivative(100.0), delta)

        //Testing derivative values for randomly generated points up to the thousandths place
        assertEquals(1.0, outputRule.getDerivative(852.628), delta)
        assertEquals(0.0, outputRule.getDerivative(-765.602), delta)
        assertEquals(1.0, outputRule.getDerivative(494.183),  delta)
        assertEquals(1.0, outputRule.getDerivative(632.561), delta)
        assertEquals(0.0, outputRule.getDerivative(-37.991), delta)
    }


    //helper function to compare the input and expected values of activations or derivatives in the form of matrices
    private fun assertMatrixEquals(expected: Matrix, actual: Matrix, delta: Double) {
        val expectedCols = expected.ncol()
        val expectedRows = expected.nrow()
        val actualCols = actual.ncol()
        val actualRows = actual.nrow()
        if((expectedCols != actualCols) || (expectedRows != actualRows)){
            fail("expected and actual matrices have different sizes! (expected: $expectedCols columns and $expectedRows rows, actual: $actualCols columns and $actualRows rows)")
        }
        else{
            val matricesEqual = expected.equals(actual, delta)
            assertTrue(matricesEqual)
        }
    }

    @Test

    fun `test array update`() {
        val delta = 1e-6
        //testing on a 1-1 network
        input5x5.activations = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0).toColumnVector()
        net.update()
        var expected = doubleArrayOf(-0.0454023, -0.158808, 0.0, 0.841192, 1.954598).toColumnVector()
        assertMatrixEquals(expected, output5x5.activations, delta)

        //Testing activation values for randomly generated points up to the thousandths place
        input5x5.activations = doubleArrayOf(-977.109, -72.505, 828.976, -363.195, 434.149).toColumnVector()
        net.update()
        expected = doubleArrayOf(0.0, 0.0, 828.976, 0.0, 434.149).toColumnVector()
        assertMatrixEquals(expected, output5x5.activations, delta)

        //testing few-many network
        input3x5.activations = doubleArrayOf(-2.0, 0.0, 1.0).toColumnVector()
        net.update()
        expected = doubleArrayOf(-0.1004284, -0.154286, 0.0, -0.154286, -0.1004284).toColumnVector()
        assertMatrixEquals(expected, output3x5.activations, delta)

        //Testing activation values for randomly generated points up to the thousandths place
        input3x5.activations = doubleArrayOf(2.649, -1.875, -5.794).toColumnVector()
        net.update()
        expected = doubleArrayOf(-0.139992, -0.00075582, -0.000000012, -0.00075582, -0.139992).toColumnVector()
        assertMatrixEquals(expected, output3x5.activations, delta)

        input3x5.activations = doubleArrayOf(-725.021, 31.823, 525.421).toColumnVector()
        net.update()
        expected = doubleArrayOf(0.0, 0.0, 178.822, 0.0, 0.0).toColumnVector()
        assertMatrixEquals(expected, output3x5.activations, delta)

        //testing many-few network
        input5x3.activations = doubleArrayOf(-3.0, -1.0, 0.5, 1.5, 5.0).toColumnVector()
        net.update()
        expected = doubleArrayOf(2.484916, 1.679795, 1.679795).toColumnVector()
        assertMatrixEquals(expected, output5x3.activations, delta)

        //Testing activation values for randomly generated points up to the thousandths place
        input5x3.activations = doubleArrayOf(0.046, -5.179, 5.941, 0.999, 0.355).toColumnVector()
        net.update()
        expected = doubleArrayOf(1.15311, -0.1580516, 4.0514452).toColumnVector()
        assertMatrixEquals(expected, output5x3.activations, delta)

        input5x3.activations = doubleArrayOf(-193.315, -563.587, 571.886, -833.933, 310.346).toColumnVector()
        net.update()
        expected = doubleArrayOf(0.0, 0.0, 0.0).toColumnVector()
        assertMatrixEquals(expected, output5x3.activations, delta)
    }


    @Test
    fun `test array derivative`() {
        val delta = 1e-6

        var input = doubleArrayOf(-5.0, -4.0, -3.0, -2.0, -1.0).toColumnVector()
        var output = outputRule.getDerivative(input)
        var expected = doubleArrayOf(-0.0000015, -0.000335123, -0.01158417, -0.0860993, -0.0829641).toColumnVector()
        assertMatrixEquals(expected, output, delta)

        input = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0).toColumnVector()
        output = outputRule.getDerivative(input)
        expected = doubleArrayOf(0.5, 1.082964, 1.086099, 1.011584, 1.000335).toColumnVector()
        assertMatrixEquals(expected, output, delta)

        //Testing derivative values for randomly generated points up to the thousandths place
        input = doubleArrayOf(-9.268, -1.116, 2.009, 6.996, -6.682).toColumnVector()
        output = outputRule.getDerivative(input)
        expected = doubleArrayOf(0.0, -0.1063385, 1.085128, 1.0, 0.0).toColumnVector()
        assertMatrixEquals(expected, output, delta)

        input = doubleArrayOf(-7.663, 9.949, 0.501, -8.804, 9.092).toColumnVector()
        output = outputRule.getDerivative(input)
        expected = doubleArrayOf(0.0, 1.0, 0.867985, 0.0, 1.0).toColumnVector()
        assertMatrixEquals(expected, output, delta)

        input = doubleArrayOf(3.896, -2.414, 2.171, -6.192, -3.499).toColumnVector()
        output = outputRule.getDerivative(input)
        expected = doubleArrayOf(1.000524, -0.0448674, 1.067893, 0.0, -0.00243121).toColumnVector()
        assertMatrixEquals(expected, output, delta)
    }
}
