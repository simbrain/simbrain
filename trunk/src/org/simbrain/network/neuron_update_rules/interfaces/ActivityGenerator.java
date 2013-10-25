package org.simbrain.network.neuron_update_rules.interfaces;

import org.simbrain.network.core.Neuron;

// TODO: Document
public interface ActivityGenerator {
	
	/**
	 * TODO: Doesn't feel like good API...
	 * Used to set values of the neuron using this rule to accommodate this
	 * rule.
	 * @param n
	 */
	public void init(Neuron n);
		
}
