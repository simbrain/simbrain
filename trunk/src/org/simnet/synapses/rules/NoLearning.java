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
public class NoLearning extends LearningRule {

	public String getName() {
		return "None";
	}
	public void apply(Synapse w) {
		}
	public String getHelp() {
		return "Do nothing; learning is off";
	}

	
}
