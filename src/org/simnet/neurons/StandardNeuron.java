/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.simnet.neurons;

import org.simnet.NetworkPreferences;
import org.simnet.interfaces.ActivationRule;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.rules.Identity;

public class StandardNeuron extends Neuron{
	
    //Rule used to update this neuron
    protected ActivationRule activationFunction = new Identity();
    
	//Amount by which to decay
	protected double decay = NetworkPreferences.getDecay();
	//Ammount by which to bias
	protected double bias = NetworkPreferences.getBias();

	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public StandardNeuron() {
	}
	
	public void update() {
		activationFunction.apply(this);
		//this.checkBounds();
	}

	/**
	 * Returns a duplicate StandardNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
		StandardNeuron n = new StandardNeuron();
		n.setActivationFunctionS(this.getActivationFunctionS());
		n.setBias(this.getBias());
		n.setDecay(this.getDecay());
		return super.duplicate(n);
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public StandardNeuron(Neuron n) {
		super(n);
	}

	public static String getName() {return "Standard";}
	
	public double getDecay() {
		return decay;
	}

	public void setDecay(double d) {
		decay = d;
	}
	
	public double getBias() {
		return bias;
	}

	public void setBias(double d) {
		bias = d;
	}

	/**
	 * Decrease (or increase) the activation of the neuron by decay, so that it converges on 0
	 */
	public void decay() {

		if (activation > 0) {
			this.activation -= decay;
		}
		if (activation < 0) {
			this.activation += decay;
		}
		if (Math.abs(activation) < decay) {
			activation = 0;
		}
	}
	/**
	 * Decay the neuron by some set value
	 * 
	 * @param amount Amount to decay this neuron by
	 */
	public void decay(double amount) {
		if (activation > 0) {
			this.activation -= amount;
		}
		if (activation < 0) {
			this.activation += amount;
		}

	}

	/**
	 * Sums the weighted signals that are sent to this node. 
	 * 
	 * @return weighted input to this node
	 */
	public double weightedInputs() {
		
		double wtdSum = 0;

		if (this.isInput())
		{		
			wtdSum = inputValue;
		}
		
		if (fanIn.size() > 0) {
			for (int j = 0; j < fanIn.size(); j++) {
				Synapse w = (Synapse) fanIn.get(j);
				Neuron source = w.getSource();
				wtdSum += w.getStrength() * source.getActivation();
			}
			wtdSum += bias;
		}
		inputValue = 0;
		return wtdSum;
	} 
	
	/**
	 * Add bias to neuron's activation level
	 */
	public void bias() {
		activation += bias;
	}
	
	/**
	 * @return Returns the currentActivationFunction.
	 */
	public ActivationRule getActivationFunction() {
		return activationFunction;
	}
	/**
	 * @param currentActivationFunction The currentActivationFunction to set.
	 */
	public void setActivationFunction(
			ActivationRule currentActivationFunction) {
		this.activationFunction = currentActivationFunction;
	}
	
	public void setActivationFunctionS(String name) {
		activationFunction = ActivationRule.getActivationFunction(name);
	}
	public String getActivationFunctionS() {
		return activationFunction.getName();
	}

}