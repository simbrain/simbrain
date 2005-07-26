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

package org.simnet.interfaces;

import java.util.ArrayList;

import org.simnet.NetworkPreferences;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.PassiveNeuron;
import org.simnet.neurons.PiecewiseLinearNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.StandardNeuron;
import org.simnet.neurons.rules.Identity;

/**
 * <b>Neuron</b> represents a node in the neural network.  Most of the "logic" of the
 * neural network occurs here, in the update function
 */

public abstract class Neuron {

	public static final int NUM_PARAMETERS = 14;

	protected String id = null;

	//Rule used to update this neuron
	protected ActivationRule activationFunction = new Identity();
	//Activation value of the neuron.  The main state variable
	protected double activation = NetworkPreferences.getActivation();
	//Minimum value this neuron can take	
	protected double lowerBound = NetworkPreferences.getNrnLowerBound();
	//Maximum value  this neuron can take
	protected double upperBound = NetworkPreferences.getNrnUpperBound();
	//Amount by which to increment or decrement neuron
	protected double increment = NetworkPreferences.getNrnIncrement();
	//Amount by which to decay
	protected double decay = NetworkPreferences.getDecay();
	//Ammount by which to bias
	protected double bias = NetworkPreferences.getBias();
	//Temporary activation value
	protected double buffer = 0;

	//Input / output info
	protected double inputValue = 0;
	protected boolean isInput = false;
	
	//Reference to network this neuron is part of
	protected Network parentNet = null;

	// Lists of connected weights.  
	protected ArrayList fanOut = new ArrayList();
	protected ArrayList fanIn = new ArrayList();

	// List of neuron types 
	private static String[] typeList = {StandardNeuron.getName(), BinaryNeuron.getName(), AdditiveNeuron.getName(),
	        LinearNeuron.getName(), PiecewiseLinearNeuron.getName(), SigmoidalNeuron.getName(), RandomNeuron.getName(),
	        PassiveNeuron.getName(), ClampedNeuron.getName()};

	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public Neuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public Neuron(Neuron n) {
		setNeuronParent(n.getNeuronParent());
		setActivation(n.getActivation());
		setActivationFunctionS(n.getActivationFunctionS());
		setBias(n.getBias());
		setDecay(n.getDecay());
		setUpperBound(n.getUpperBound());
		setLowerBound(n.getLowerBound());
		setInputValue(n.getInputValue());
	}
	

	/**
	 * Creates a duplicate of this neuron; used in copy/paste
	 * 
	 * @return duplicate neuron
	 */
	public Neuron duplicate(Neuron n) {
		n.setNeuronParent(this.getNeuronParent());
		n.setActivation(this.getActivation());
		n.setActivationFunctionS(this.getActivationFunctionS());
		n.setBias(this.getBias());
		n.setDecay(this.getDecay());
		n.setUpperBound(this.getUpperBound());
		n.setLowerBound(this.getLowerBound());
		return n;
	}


	public abstract Neuron duplicate();
	public abstract void update();


	/**
	 * Utility method to see if an array of names (from the world) contains a target string
	 * 
	 * @param src the list of Strings
	 * @param target the string to check for
	 * @return whether src is contained in target or not
	 */
	public boolean containsString(ArrayList src, String target) {
		
		boolean ret = false;
		java.util.Iterator it = src.iterator();
		while (it.hasNext()) {
			if (target.equals((String)it.next())); {
				ret = true;
			}
		}
		return ret;
	}
	

	public void setActivation(double act) {
		activation = act;
	}
	public double getActivation() {
		return activation;
	}

	public String getId() {
		return id;
	}

	public void setId(String theName) {
		id = theName;
	}

	public double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(double d) {
		upperBound = d;
	}

	public double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(double d) {
		lowerBound = d;
	}

	public double getIncrement() {
		return increment;
	}

	public void setIncrement(double d) {
		increment = d;
	}

	public double getDecay() {
		return decay;
	}

	public void setDecay(double d) {
		decay = d;
	}

	public ArrayList getFanIn() {
		return fanIn;
	}

	public ArrayList getFanOut() {
		return fanOut;
	}

	/**
	 * @param fanIn The fanIn to set.
	 */
	public void setFanIn(ArrayList fanIn) {
		this.fanIn = fanIn;
	}
	/**
	 * @param fanOut The fanOut to set.
	 */
	public void setFanOut(ArrayList fanOut) {
		this.fanOut = fanOut;
	}
	
	public double getBias() {
		return bias;
	}

	public void setBias(double d) {
		bias = d;
	}
		
	/**
	 * Increment this neuron by increment
	 */
	public void incrementActivation() {
		if(activation < upperBound)  {
			activation += increment;
		}
	}

	/**
	 * Decrement this neuron by increment
	 */
	public void decrementActivation() {
		if(activation > lowerBound)  {
			activation -= increment;
		}
	}

	/**
	 * Connect this neuron to target neuron via a weight
	 * 
	 * @param target the connnection between this neuron and a target neuron
	 */
	public void addTarget(Synapse target) {
		fanOut.add(target);
	}

	/**
	 * Connect this neuron to source neuron via a weight
	 * 
	 * @param source the connnection between this neuron and a source neuron
	 */
	public void addSource(Synapse source) {
		fanIn.add(source);
	}
	/**
	 * Add specified amount of activation to this neuron
	 * 
	 * @param amount amount to add to this neuron
	 */
	public void addActivation(double amount) {
		activation += amount;
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
	 * Randomize this neuron to a value between upperBound and lowerBound
	 */
	public void randomize() {
		setActivation((upperBound - lowerBound) * Math.random() + lowerBound);
//		if (getBias() != 0) {
//			setBias((upperBound - lowerBound) * Math.random() + lowerBound);			
//		}
	}
	
	/**
	 * Randomize this neuron to a value between upperBound and lowerBound
	 */
	public void randomizeBuffer() {
		setBuffer((upperBound - lowerBound) * Math.random() + lowerBound);
	}

	/**
	 *  Update all neurons n this neuron is connected to, by adding current activation times the connection-weight 
	 *  NOT CURRENTLY USED
	 */
	public void updateConnectedOutward() {
		// Update connected weights
		if (fanOut.size() > 0) {
			for (int j = 0; j < fanOut.size(); j++) {
				Synapse w = (Synapse) fanOut.get(j);
				Neuron target = w.getTarget();
				target.setActivation(w.getStrength() * activation);
				target.checkBounds();
			}
		}

	}

	/**
	 * Check if this neuron is connected to a given weight
	 * 
	 * @param w weight to check
	 * @return true if this neuron has w in its fan_in or fan_out.  
	 */
	public boolean connectedToWeight(Synapse w) {
		if (fanOut.size() > 0) {
			for (int j = 0; j < fanOut.size(); j++) {
				Synapse out_w = (Synapse) fanOut.get(j);
				if (w.equals(out_w)) {
					return true;
				}
			}
		}
		if (fanIn.size() > 0) {
			for (int j = 0; j < fanIn.size(); j++) {
				Synapse in_w = (Synapse) fanIn.get(j);
				if (w.equals(in_w)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Round the activation level of this neuron off to a specified precision
	 * 
	 * @param precision precision to round this neuron's activaion off to
	 */
	public void round(int precision) {
		setActivation(Network.round(getActivation(), precision));
	}
	
	/**
	 * If activation is above or below its bounds set it to those bounds
	 */
	public void checkBounds() {

		if (activation > upperBound) {
			activation = upperBound;
		}

		if (activation < lowerBound) {
			activation = lowerBound;
		}
	}

	/**
	 * Sends relevant information about the network to standard output.
	 * TODO: Change to toString()
	 */
	public void debug() {

		System.out.println("neuron " + id);
		System.out.println("fan in");
		for (int i = 0; i < fanIn.size(); i++) {
			Synapse tempRef = (Synapse) fanIn.get(i);
			System.out.println("fanIn [" + i + "]:" + tempRef);
		}
		System.out.println("fan out");
		for (int i = 0; i < fanOut.size(); i++) {
			Synapse tempRef = (Synapse) fanOut.get(i);
			System.out.println("fanOut [" + i + "]:" + tempRef);
		}
	}

	/**
	 * @return reference to the Network object this neuron is part of
	 */
	public Network getNeuronParent() {
		return parentNet;
	}

	/**
	 * @param network reference to the Network object this neuron is part of.  
	 */
	public void setNeuronParent(Network network) {
		parentNet = network;
	}
	

	/**
	 * Temporary buffer which can be used for algorithms which shoudl not 
	 * depend on the order in which 
	 * neurons are updated
	 * 
	 * @param d temporary value
	 */
	public void setBuffer(double d) {
		buffer = d;
	}
	
	/**
	 * @return Returns the current value in the buffer.
	 */
	public double getBuffer() {
		return buffer;
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
	/**
	 * @return Returns the inputValue.
	 */
	public double getInputValue() {
		return inputValue;
	}
	/**
	 * @param inputValue The inputValue to set.
	 */
	public void setInputValue(double inputValue) {
		this.inputValue = inputValue;
	}

	/**
	 * @return the name of the class of this network
	 */
	public String getType() {
		return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
	}
		
	/**
	 * @return Returns the typeList.
	 */
	public static String[] getTypeList() {
		return typeList;
	}
	/**
	 * @param typeList The typeList to set.
	 */
	public static void setTypeList(String[] typeList) {
		Neuron.typeList = typeList;
	}
	/**
	 * Helper function for combo boxes.  Associates strings with indices.
	 */	
	public static int getNeuronTypeIndex(String type) {
		for (int i = 0; i < typeList.length; i++) {
			if (type.equals(typeList[i])) {
				return i;
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @return the sum of the incoming weights to this nueron
	 */
	public double getSummedIncomingWeights() {
		double ret = 0;
		for (int i = 0; i < fanIn.size(); i++) {
			Synapse tempRef = (Synapse) fanIn.get(i);
			ret+= tempRef.getStrength();
		}
		return ret;
		
	}
	/**
	 * @return Returns the isInput.
	 */
	public boolean isInput() {
		return isInput;
	}
	/**
	 * @param isInput The isInput to set.
	 */
	public void setInput(boolean isInput) {
		this.isInput = isInput;
	}
}