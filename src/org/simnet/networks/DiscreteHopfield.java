 /*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.*;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.*;
import org.simnet.neurons.rules.*;
import org.simnet.synapses.*;

import java.util.*;

/**
 * Discrete hopfield networks
 */
public class DiscreteHopfield extends Hopfield {
	
	public static final int RANDOM_UPDATE = 0;
	public static final int SEQUENTIAL_UPDATE = 1;
	
	private int update_order = SEQUENTIAL_UPDATE;

	public DiscreteHopfield() {
	}
	
	public DiscreteHopfield(int numNeurons) {
		//Create the neurons
		for(int i = 0; i < numNeurons; i++) {
			BinaryNeuron n = new BinaryNeuron();
			addNeuron(n);
		}
		
		this.createConnections();
				
	}
	
	/**
	 * Update nodes randomly or sequentially
	 * 
	 */
	public void update() {		
		int n_count = getNeuronCount();
		int j; Neuron n;
		for (int i = 0; i < n_count; i++) {
			j = (int)(Math.random() * n_count);
			if (update_order == RANDOM_UPDATE) {
				n = (Neuron) neuronList.get(j);
			} else {
				n = (Neuron) neuronList.get(i);
			}
			n.update();	
			n.setActivation(n.getBuffer());
		}
	}
}
