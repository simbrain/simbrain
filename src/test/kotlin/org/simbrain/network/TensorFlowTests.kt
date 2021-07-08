package org.simbrain.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.matrix.WeightMatrixTest
import org.tensorflow.Tensor
import java.nio.FloatBuffer

/**
 * Compare the code in [WeightMatrixTest]
 */
class TensorFlowTests {

    @Test
    fun `tensor flow basics`() {

        // Initial code to create a 2d matrix of floats
        val matrix = arrayOf(
                floatArrayOf(1.0f,2.0f,3.0f),
                floatArrayOf(2.0f,3.3f,4.0f));
        //println(matrix.contentDeepToString())
        val tensor = Tensor.create(matrix)
        //println(tensor.convertTensorToMultiDimArray().contentDeepToString())

        // Create a 2x3 zero matrix
        val shape = longArrayOf(2,3)
        val sampData: FloatBuffer = FloatBuffer.allocate(6)
        val matrix_of_zeros = Tensor.create(shape,sampData);
        // println(matrix_of_zeros.convertTensorToMultiDimArray().contentDeepToString());
        var tensor_to_matrix =  Array(2){FloatArray(3) {0.0f}}
        matrix_of_zeros.copyTo(tensor_to_matrix);

        // Get its shape
        // println(matrix_of_zeros)

        // Create a 2x3 matrix of ones
        val sampData1: FloatBuffer = FloatBuffer.allocate(6)
        sampData1.put(0,1.0f)
        sampData1.put(1, 1.0f)
        sampData1.put(2,1.0f)
        sampData1.put(3, 1.0f)
        sampData1.put(4,1.0f)
        sampData1.put(5, 1.0f)
        val matrix_of_ones = Tensor.create(shape,sampData1)
        // println(matrix_of_ones.convertTensorToMultiDimArray().contentDeepToString())

        // Confirm sum of entries is 6
        matrix_of_ones.copyTo(tensor_to_matrix)
        var sum = 0.0f;
        for (i in tensor_to_matrix.indices) {
            for (j in tensor_to_matrix[i].indices) {
                sum += tensor_to_matrix[i][j]
            }
        }
        assertEquals(6.0f, sum)

        // Set the value of entry 1,3 to 3
        tensor_to_matrix[0][2] = 3.0f;

        // Retrieve the value of that entry and confirm it is 3
        // println(tensor_to_matrix[0][2])

        // Perform a matrix multiplication using simple values and confirm correct outputs
        //val temp = MatrixDiagV2.create()
        // Perform a dot product and confirm correct outputs

    }


}