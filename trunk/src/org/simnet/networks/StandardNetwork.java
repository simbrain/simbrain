/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.Network;
import org.simnet.neurons.StandardNeuron;

/**
 * @author yoshimi
 *
 * Container network serves as a high-level container for other networks and neurons.  
 * It contains a list of neurons as well as a list of networks.  When 
 * building simulations in which multiple networks interact, this should be the
 * top-level network which contains the rest.
 */
public class StandardNetwork extends Network {
	
	public StandardNetwork() {
	}
	
	public StandardNetwork(int n_units) {
		for(int i = 0; i < n_units; i++) {
			this.addNeuron(new StandardNeuron());
		}
	}
	
	public void init() {
		super.init();
	}
	/**
	 * The core update function of the neural network.  Calls
	 * the current update function on each neuron,
	 * decays all the neurons, and checks their bounds. 
	 */
	public void update() {
		updateAllNeurons();
		updateAllWeights();
		decayAll();
		checkAllBounds();
		
	}
}
