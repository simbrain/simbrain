package org.simnet.networks;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.neurons.LinearNeuron;

public class Competitive extends Network {

	public Competitive(int numNeurons) {
		super();
		for(int i = 0; i < numNeurons; i++) {
			this.addNeuron(new LinearNeuron());
		}
	}
	
	public void update() {
		
		//Winner Take All for Neurons
		updateAllNeurons();
		double max = 0;
		int winner = 0;
		Neuron win = null;
		
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			if (n.getActivation() > max) {
				max = n.getActivation();
				winner = i;
			}
		}
		
		for (int i = 0; i < neuronList.size(); i++) {
			if (i == winner) {
				win = ((Neuron)neuronList.get(i));
				win.setActivation(1);
			} else ((Neuron)neuronList.get(i)).setActivation(0);
		}
		
		// Now do the weights
		

	}

}
