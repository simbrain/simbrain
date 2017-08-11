package org.simbrain.network.neuron_update_rules;

import org.jblas.DoubleMatrix;

public interface TransferFunction {

	public void applyFunctionInPlace(DoubleMatrix input);
	
	
	public void applyFunction(DoubleMatrix input, DoubleMatrix output);
	
}
