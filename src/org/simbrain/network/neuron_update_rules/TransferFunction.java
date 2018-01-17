package org.simbrain.network.neuron_update_rules;

import org.jblas.DoubleMatrix;

/**
 * 
 * Interface for neuron update rules (referred to as the "parent rule" here) to
 * be used with an array-based library backing, currently jblas. Methods to
 * apply the parent rule to an array, and to apply the derivative of the parent
 * rule to an array are supplied. Useful for backprop.
 *
 * @author Zoë Tosi
 *
 */
public interface TransferFunction {

    /**
     * Apply parent rule in place to net input.
     *
     * @param input the matrix to be mutated.
     */
    void applyFunctionInPlace(DoubleMatrix input);

    /**
     * Applies the parent rule to the input and writes the result to output.
     *
     * @param input usually the net input
     * @param output the output to be mutated
     */
    void applyFunction(DoubleMatrix input, DoubleMatrix output);

    /**
     * Apply the derivative of the parent rule to the input and write the result
     * to output. Parent rule = f; input = x output = y. Compute f'(x) and store
     * in y.
     *
     * @param input the net input x
     * @param output the output y to be mutated
     */
    void getDerivative(DoubleMatrix input, DoubleMatrix output);

    /**
     * Apply the transfer function to the input vector and store the result in output. Then apply the derivative
     * of the transfer function to the input and store the result of that in derivative.
     * @param input The input vector.
     * @param output The output vector.
     * @param derivative The derivative vector.
     */
    void applyFunctionAndDerivative(DoubleMatrix input, DoubleMatrix output, DoubleMatrix derivative);
}
