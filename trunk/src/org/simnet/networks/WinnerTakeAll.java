/*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.LinearNeuron;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class WinnerTakeAll extends Network {

	private double win_value = 1;
	private double lose_value = 0;
	
	public WinnerTakeAll() {
		super();
	}
	
	public WinnerTakeAll(int numNeurons) {
		super();
		for(int i = 0; i < numNeurons; i++) {
			this.addNeuron(new LinearNeuron());
		}
	}
	
	public void update() {
		updateAllNeurons();
		double max = 0;
		int winner = 0;
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			if (n.getActivation() > max) {
				max = n.getActivation();
				winner = i;
			}
		}
		
		for (int i = 0; i < neuronList.size(); i++) {
			if (i == winner) {
				((Neuron)neuronList.get(i)).setActivation(win_value);
			} else ((Neuron)neuronList.get(i)).setActivation(lose_value);
		}
		
	}
}
