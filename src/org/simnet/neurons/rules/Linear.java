/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.neurons.rules;

import org.simnet.interfaces.*;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Linear extends ActivationRule {

	protected void apply(Neuron n) {
		n.setBuffer(n.weightedInputs() * ((n.getActivation() - n.getLowerBound())/(n.getUpperBound() - n.getLowerBound())));
	}			
	
	public String getHelp() {
		return "Linear: the output is a linear map that takes the node's lower-bound to 0 and upper-bound to this node's signal value";
	}		
}
