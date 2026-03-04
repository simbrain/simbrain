package org.simbrain.network.trainers

/**
 * Dense (fully-connected) layer operations using pure DoubleArrays.
 * All operations work on flat arrays with explicit row/col dimensions.
 */
object DoubleArrayOps {

    /**
     * Dense forward: output = weights * input + biases.
     *
     * @param weights flat array of size [rows] x [cols] (row-major)
     * @param rows    number of output neurons (rows of weight matrix)
     * @param cols    number of input neurons (cols of weight matrix)
     * @param input   input vector (length = cols)
     * @param biases  bias vector (length = rows)
     * @param output  output vector (length = rows), overwritten
     */
    fun matVecMultiply(
        weights: DoubleArray, rows: Int, cols: Int,
        input: DoubleArray, biases: DoubleArray, output: DoubleArray
    ) {
        for (r in 0 until rows) {
            var sum = biases[r]
            val offset = r * cols
            for (c in 0 until cols) {
                sum += weights[offset + c] * input[c]
            }
            output[r] = sum
        }
    }

    /**
     * Transpose-vector multiply: output = weights^T * grad.
     * Used to backpropagate error through a dense layer.
     *
     * @param weights flat array of size [rows] x [cols] (row-major)
     * @param rows    number of output neurons
     * @param cols    number of input neurons
     * @param grad    gradient vector from downstream (length = rows)
     * @param output  gradient for input layer (length = cols), overwritten
     */
    fun transposeVecMultiply(
        weights: DoubleArray, rows: Int, cols: Int,
        grad: DoubleArray, output: DoubleArray
    ) {
        output.fill(0.0)
        for (r in 0 until rows) {
            val g = grad[r]
            if (g == 0.0) continue
            val offset = r * cols
            for (c in 0 until cols) {
                output[c] += weights[offset + c] * g
            }
        }
    }

    /**
     * Outer product accumulate: weightGrad += error (outer) input.
     * Accumulates the weight gradient for a dense layer.
     *
     * @param error      error/gradient for output layer (length = rows)
     * @param input      input activations (length = cols)
     * @param weightGrad accumulated weight gradients (length = rows * cols, row-major)
     * @param rows       number of output neurons
     * @param cols       number of input neurons
     */
    fun outerProductAccumulate(
        error: DoubleArray, input: DoubleArray,
        weightGrad: DoubleArray, rows: Int, cols: Int
    ) {
        for (r in 0 until rows) {
            val e = error[r]
            if (e == 0.0) continue
            val offset = r * cols
            for (c in 0 until cols) {
                weightGrad[offset + c] += e * input[c]
            }
        }
    }
}
