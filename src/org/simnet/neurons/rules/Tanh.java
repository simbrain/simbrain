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
public class Tanh extends ActivationRule {

	public void apply(Neuron n) {
		double act = n.weightedInputs();
		n.setBuffer(getValue(act));
	}			
	public String getHelp() {
		return "Tanh: the output signal is a hyperbolic tangent (between -1 and 1) of its activation. Resembels a sigmoidal.";
	}				

	public static double getValue(double input) {

		return (Math.exp(input) - (Math.exp(-input))) / (Math.exp(input) + Math.exp(-input));
	}
	public String getName() { return "Tanh";}
	
	
}
