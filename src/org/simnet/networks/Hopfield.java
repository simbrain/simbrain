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
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Hopfield extends Network {

	public Hopfield() {
	}
	
	public Hopfield(int numNeurons) {
		//Create the neurons
		for(int i = 0; i < numNeurons; i++) {
			BinaryNeuron n = new BinaryNeuron();
			addNeuron(n);
		}
		
		//Create full symmetric connections without self-connections
		for(int i = 0; i < numNeurons; i++) {
			for(int j = 0; j < i; j++) {
				StandardSynapse w = new StandardSynapse();
				w.setUpperBound(10);
				w.setLowerBound(-10);
				w.randomize();
				w.setSource(this.getNeuron(i));
				w.setTarget(this.getNeuron(j));
				addWeight(w);
				
				StandardSynapse w2 = new StandardSynapse();
				w2.setUpperBound(10);
				w2.setLowerBound(-10);
				w2.setStrength(w.getStrength());
				w2.setSource(this.getNeuron(j));
				w2.setTarget(this.getNeuron(i));
				addWeight(w2);

			}
			
		}
				
		
	}
	
	public void train() {
		for(int i = 0; i < this.getWeightCount(); i++) {
			//Must use buffer
			Synapse w = this.getWeight(i);
			Neuron src = w.getSource();
			Neuron tar = w.getTarget();
			w.setStrength((2 * src.getActivation() - 1) * (2 * tar.getActivation() - 1));
		}
	}
	
	public void update() {
		updateAllNeurons();
	}
}
