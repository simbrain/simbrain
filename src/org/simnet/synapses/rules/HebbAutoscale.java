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
public class HebbAutoscale extends LearningRule {

	protected void apply(Synapse w) {
		double src = w.getSource().getActivation();
		double tar = w.getTarget().getActivation();
		double wt = w.getStrength();
		w.setStrength(wt + (w.getMomentum() * (src * tar - Math.pow(tar,2)* wt)));
	}
	
	public String getHelp() {
		return "Hebbian learning where an autoscale term (target activation squared times current weight) is subtracted, to prevent weight values from maxing out too quickly";
	}
	
}
