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
public class Threshold extends ActivationRule {

	protected void apply(Neuron n) {
		if (n.weightedInputs() >= n.getActivationThreshold()) {
			n.setBuffer(n.getUpperBound());
		} else {
			n.setBuffer(n.getLowerBound());
		}
	}

	public boolean usesThreshold() {
		return true;
	}

	public String getHelp() {
		return "Threshold: If activation is greater than activation threshold, then set activation to upper bound; else to lower bound."
			+ "NOTE: Not to be confused with the output threshold function, which is generally preferred to this one.";
	}
}
