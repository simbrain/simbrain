 /*
 * Created on Sep 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simnet.networks;

import org.simnet.interfaces.Neuron;
import org.simnet.neurons.AdditiveNeuron;

/**
 * 
 * <b>ContinuousHopfield</b>
 */
public class ContinuousHopfield extends Hopfield {
	
	public ContinuousHopfield() {
		super();
	}
	
	public ContinuousHopfield(int numNeurons) {
		super();
		//Create the neurons
		for(int i = 0; i < numNeurons; i++) {
			AdditiveNeuron n = new AdditiveNeuron();
			addNeuron(n);
		}
		
		this.createConnections();
	}
	
	/**
	 * Update nodes using a buffer
	 * 
	 */
	public void update() {		

		for (int i = 0; i < getNeuronCount(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.update();		
		}
		for (int i = 0; i < getNeuronCount(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.setActivation(n.getBuffer());
		}
	
	}

}
