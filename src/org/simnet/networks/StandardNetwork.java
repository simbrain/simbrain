/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.Network;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StandardNetwork extends Network {

	public StandardNetwork() {
		System.out.println("Standard Network");
	}
	/**
	 * The core update function of the neural network.  Calls
	 * the current update function on each neuron,
	 * decays all the neurons, and checks their bounds. 
	 */
	public void update() {
		time++;
		updateAllNeurons();
		updateAllWeights();
		decayAll();
		checkAllBounds();
		if (isRoundingOff()) {
			roundAll();
		} 
	}
}
