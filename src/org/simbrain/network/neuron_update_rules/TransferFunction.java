package org.simbrain.network.neuron_update_rules;

import org.jblas.DoubleMatrix;

/**
 * 
 * Interface for neuron update rules (referred to as the "parent rule" here) to
 * be used with an array-based library backing, currently jblas. Methods to
 * apply the parent rule to an array, and to apply the derivative of the parent
 * rule to an array are supplied. Useful for backprop.
 *
 * @author Zach Tosi
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

}
