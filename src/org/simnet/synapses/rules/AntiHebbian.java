/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.synapses.rules;

import org.simnet.interfaces.*;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AntiHebbian extends LearningRule {

	public String getName() {
		return "AntiHebbian";
	}
	
	//Currently no momentum...
	public void apply(Synapse w) {
		Neuron src = w.getSource();
		Neuron trg = w.getTarget();
		double val = w.getStrength() - (src.getActivation() * trg.getActivation());
		if (val <= 0) 
			w.setStrength(0);
		else w.setStrength(val);
		
	}
	public String getHelp() {
		return "Anti-Hebbian Learning: Subtract the quantity momentum * source.activation * target.activation to this weight on each update cycle";
	}
}
