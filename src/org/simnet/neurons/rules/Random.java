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
public class Random extends ActivationRule {

	protected void apply(Neuron n) {
		n.randomizeBuffer();
	}
	public String getHelp() {
		return "Randomize: Activation is determined by a uniform random distribution between the upper and the lower bounds.";
	}
	
}
