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
 * Continuous hopfield net
 */
public class ContinuousHopfield extends Hopfield {

	private double time_step = .1;
	
	public ContinuousHopfield() {
	}
	
	public ContinuousHopfield(int numNeurons) {
		//Create the neurons
		for(int i = 0; i < numNeurons; i++) {
			AdditiveNeuron n = new AdditiveNeuron();
			addNeuron(n);
			n.setTimeStep(time_step);
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
	
	/**
	 * @return Returns the time_step.
	 */
	public double getTime_step() {
		return time_step;
	}
	/**
	 * @param time_step The time_step to set.
	 */
	public void setTime_step(double time_step) {
		this.time_step = time_step;
	}

}
