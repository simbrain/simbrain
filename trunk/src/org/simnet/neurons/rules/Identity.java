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
public class Identity extends ActivationRule {

	public void apply(Neuron n) {
		n.setBuffer(n.weightedInputs());
	}
	public String getHelp() {
		return "Simmply return a weighted sum of inputs";
	}

	public String getName() { return "Identity";}

}
