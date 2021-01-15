package org.simbrain.network

import junit.framework.Assert.assertEquals
import org.jetbrains.kotlinx.dl.api.extension.convertTensorToMultiDimArray
import org.junit.Test
import org.simbrain.network.dl4j.WeightMatrixTest
import org.tensorflow.Tensor

/**
 * Compare the code in [WeightMatrixTest]
 */
class TensorFlowTests {

    @Test
    fun `learningAboutTests`() {

        var a = 10

        // Check basic assignment
        assertEquals(10, a)

        // 20 = 10+10
        assertEquals(20,a+a)

        // 11 = 10+1
        a+=1
        assertEquals(11, a)
    }

    @Test
    fun `tensor flow basics`() {

        // Initial code to create a 2d matrix of floats
        val matrix = arrayOf(
                floatArrayOf(1.0f,2.0f,3.0f),
                floatArrayOf(2.0f,3.3f,4.0f));
        println(matrix.contentDeepToString())
        val tensor = Tensor.create(matrix)
        println(tensor.convertTensorToMultiDimArray().contentDeepToString())

        // Create a 2x3 zero matrix
        // Get its shape
        // Create a 2x3 matrix of ones
        // Confirm sum of entries is 6
        // Set the value of entry 1,3 to 3
        // Retrieve the value of that entry and confir it is 3
        // Perform a matrix multiplication using simple values and confirm correct outputs
        // Perform a dot product and confirm correct outputs


    }


}