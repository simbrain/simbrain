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
import org.simnet.interfaces.*;
import org.simnet.neurons.rules.*;

/**
 * <b>Neuron</b> represents a node in the neural network.  Most of the "logic" of the
 * neural network occurs here, in the update function
 */

public class Neuron {

	// When new fields are added, the following must be changed
	//		0) The NUM_PARAMETERS field
	// 		1) setParameters, called by the Neuron constructor.  See below
	//		2) writeNet, in NetworkSerializer
	//		3) DialogNeuron class
	//
	// Scripts may also have  to be used to update the network files
	//
	// TODO: Find a cleaner way to to do this, which does not require so many changes.. USE XML!

	public static final int NUM_PARAMETERS = 14;

	private String name = null;
	private ActivationRule currentActivationFunction = new Linear();
	private boolean isInput = false;
	//True if this is an input neuron, which receives input from an environment
	private boolean isOutput = false;
	//True if this is an output neuron, which sends output to an environment
	private double activation = NetworkPreferences.getActivation();
	//Activation value of the neuron.  The main state variable
	private double lowerBound = NetworkPreferences.getNrnLowerBound();
	//Minimum value this neuron can take
	private double upperBound = NetworkPreferences.getNrnUpperBound();
	//Maximum value  this neuron can take
	private double outputSignal = NetworkPreferences.getOutputSignal();
	//Strength of signal to other neurons.  Used, canonically, with a threshold output function
	private double outputThreshold = NetworkPreferences.getOutputThreshold();
	//Threshold for use in output function
	private double activationThreshold =
		NetworkPreferences.getActivationThreshold();
	//Threshold for use in activation function (less common)
	private double increment = NetworkPreferences.getNrnIncrement();
	//Amount by which to increment or decrement neuron
	private double decay = NetworkPreferences.getDecay();
	//Amount by which to decay
	private double bias = NetworkPreferences.getBias();
	//Ammount by which to bias
	private double buffer = 0;
	//Temporary activation value

	
	/** label which describes the "movement" this node corresponds to */
	private String outputLabel = "";	
	private String inputLabel = "";
	
	private Network parentNet = null;

	// Lists of connected weights.  
	private ArrayList fanOut = new ArrayList();
	private ArrayList fanIn = new ArrayList();

	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public Neuron() {
		System.out.println("Neuron");
	}

	/**
	 * Static factory method used in lieu of clone, which creates duplicate Neurons.
	 * Used, for example, in copy/paste.
	 * 
	 * @param n neuron to duplicate
	 * @return duplicate neuron
	 */
	public static Neuron getDuplicate(Neuron n) {
		Neuron ret = new Neuron();
		ret.setParameters(n.getParameters());
		ret.setParentNet(n.getParentNet());
		return ret;
	}

	/**
	 * Construct a neuron using an array of parameter values
	 * @param values
	 */
	public Neuron(String[] values) {
		if (values.length < NUM_PARAMETERS) {
			System.out.println("Problem reading neuron parameters");
			return;
		}

		setParameters(values);
	}

	/**
	 * Set specified values of this neuron by passing a string of parameter values, with nulls for
	 * values not to change.
	 * 
	 * @param values a set of new values for this neuron
	 */
	public void setParameters(String[] values) {
		//System.out.println("set:" + java.util.Arrays.asList(values));
		if (values[0] != null)
			name = values[0];
		if (values[1] != null) {
			//For compatibility with older versions of Simbrain	
			if(values[1].equalsIgnoreCase("false") || values[1].equalsIgnoreCase("true")) {
				isInput = Boolean.valueOf(values[1]).booleanValue();
			}
			 else {
			 	String input = values[1]; 				
				if ((input.equals("not_input")) || (input.equals("0")) || (input.equals(""))) {
					isInput= false;
				} else {
					isInput = true; 
					inputLabel = values[1];
				}
			}
		}
		if (values[2] != null) {
			//For compatibility with older versions of Simbrain	
			if(values[2].equalsIgnoreCase("false") || values[2].equalsIgnoreCase("true")) {
				isOutput = Boolean.valueOf(values[2]).booleanValue();
			}
			else {
				outputLabel = values[2];				
				if ((outputLabel.equals("not_output")) || (outputLabel.equals("0")) || (outputLabel.equals(""))) {
					isOutput = false;
				} else {
					isOutput = true;
				}
			} //TODO This must change.  Must get rid of true /false
		}
		if (values[3] != null)
			currentActivationFunction = new Linear();
		if (values[5] != null)
			activation = Double.parseDouble(values[5]);
		if (values[6] != null)
			lowerBound = Double.parseDouble(values[6]);
		if (values[7] != null)
			upperBound = Double.parseDouble(values[7]);
		if (values[8] != null)
			outputSignal = Double.parseDouble(values[8]);
		if (values[9] != null)
			outputThreshold = Double.parseDouble(values[9]);
		if (values[10] != null)
			activationThreshold = Double.parseDouble(values[10]);
		if (values[11] != null)
			increment = Double.parseDouble(values[11]);
		if (values[12] != null)
			decay = Double.parseDouble(values[12]);
		if (values[13] != null)
			bias = Double.parseDouble(values[13]);
	}

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
	
	/**
	 * Get an array of strings describing this neuron's state and paramter settings
	 * 
	 * @param values an array of Strings containing state and parameter settings for this neuron
	 * 
	 */	
	public String[] getParameters() {
		//TODO: Call in net serializer?
		String[] retString = {
			"",
			inputLabel,
			getOutputLabel(),
			getActivationFunction().getName(),
			"",
			Double.toString(getActivation()),
			Double.toString(getLowerBound()),
			Double.toString(getUpperBound()),
			Double.toString(getOutputSignal()),
			Double.toString(getOutputThreshold()),
			Double.toString(getActivationThreshold()),
			Double.toString(getIncrement()),
			Double.toString(getDecay()),
			Double.toString(getBias())
		};
		//System.out.println("get:" + Arrays.asList(retString));
		return retString;
	}

	public ActivationRule getActivationFunction() {
		return currentActivationFunction;
	}
	public void setActivationFunction(ActivationRule actFunction) {
		currentActivationFunction = actFunction;
	}


	public void setActivation(double act) {
		activation = act;
	}
	public double getActivation() {
		return activation;
	}

	public String getName() {
		return name;
	}

	public void setName(String theName) {
		name = theName;
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
	
	public double getActivationThreshold() {
		return activationThreshold;
	}

	public double getBias() {
		return bias;
	}

	public double getOutputSignal() {
		return outputSignal;
	}

	public double getOutputThreshold() {
		return outputThreshold;
	}

	public void setActivationThreshold(double d) {
		activationThreshold = d;
	}

	public void setBias(double d) {
		bias = d;
	}

	public void setOutputSignal(double d) {
		outputSignal = d;
	}

	public void setOutputThreshold(double d) {
		outputThreshold = d;
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
	 *  Main neuron update method.  Calls an activation rule.  See PDP 1, p. 52
	 */
	public void update() {
		//TODO: catch an "activation function not recognized" exception and 
		// say what the unrecognized activation function was
		//currentActivationFunction.apply(this);
	}

	/**
	 * Sums the weighted signals that are sent to this node. 
	 * 
	 * @return weighted input to this node
	 */
	public double weightedInputs() {
		double wtdSum = 0;
		if (fanIn.size() > 0) {
			for (int j = 0; j < fanIn.size(); j++) {
				Synapse w = (Synapse) fanIn.get(j);
				Neuron source = w.getSource();
				wtdSum += w.getStrength() * source.getOutput();
			}
			wtdSum += bias;
		}
		return wtdSum;
	} 

	/**
	 * Call this neuron's output function.  Should be called by a target neuron
	 */
	public double getOutput() {
		return 0;
		//return currentOutputFunction.calc(this);
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

		System.out.println("neuron " + name);
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
	public Network getParentNet() {
		return parentNet;
	}

	/**
	 * @param network reference to the Network object this neuron is part of.  
	 */
	public void setParentNet(Network network) {
		parentNet = network;
	}
	

	/**
	 * Set the activation value of a buffer first, before committing the value, 
	 * so that the network algorithms don't depend on the order in which 
	 * neurons are updated
	 * 
	 * @param d temporarl activation value
	 */
	public void setBuffer(double d) {
		buffer = d;
	}

	/**
	 * Set the activation level of the neuron to the
	 * activation level of the temporary buffer 
	 */
	public void commitBuffer() {
		activation = buffer;
	}
	

	//TODO: Input / Output interface needs to be cleaned up
	
	public String getOutputLabel() {
		if (this.isOutput() == false) {
			return "not_output";
		}
		return outputLabel;
	}
	
	public String getInputLabel() {
		if (this.isInput() == false) {
			return "not_input";
		}
		return inputLabel;
	}
	
	public void setInputLabel(String input) {
		inputLabel= input;
	}
	
	public void setOutputLabel(String output) {
		outputLabel = output;
	}

	public boolean isInput() {
		return isInput;
	}
	public boolean isOutput() {
		return isOutput;
	}

	public boolean getInput() {
		return isInput;
	}

	public void setInput(boolean in) {
		isInput = in;
		if (in == true) {
			parentNet.addInputNeuron(this);
		} 
		else {
			parentNet.removeInputNeuron(this);
			inputLabel = "not_input";
		}
	}
	
	public void setOutput(boolean out) {
		isOutput = out;
		if (out == true) {
			parentNet.addOutputNeuron(this);
		} 
		else {
			parentNet.removeOutputNeuron(this);
			outputLabel = "not_output";
		}
	}
	

}