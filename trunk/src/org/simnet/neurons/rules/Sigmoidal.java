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
public class Sigmoidal extends ActivationRule {

	public void apply(Neuron n) {
		n.setBuffer(1 / (1 + Math.exp(-n.weightedInputs())));
	}			
	public String getHelp() {
		return "Sigmoidal: activation of the node is a sigmoidal function (between 0 and 1) of weighted inputs. Note ";
	}	
	public String getName() { return "Sigmoidal";}

	
}
