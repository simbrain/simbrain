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
import java.util.Collection;

import org.simnet.networks.StandardNetwork;


/**
 * <b>Network</b> provides core neural network functionality and is the
 * the main API for external calls. Network objects are sets of neurons and 
 * weights connecting them. Much of the  actual update and  learning logic
 * occurs (currently) in the individual nodes.
 * 
 */
public abstract class Network {

	//
	// TODO: 	Add a command-line interface for creating and testing simple networks 
	// 			independently of the GUI.
	//
	// TODO: 	Make saving /opening of files possible from here.  Change file format
	//			so that first character indicates what kind of object is being read.
	//

	protected ArrayList neuronList = new ArrayList();
	protected ArrayList weightList = new ArrayList();
	protected ArrayList inputList = new ArrayList();
	protected ArrayList outputList = new ArrayList();
	
	protected int time = 0; 	// Keeps track of time
	
	private boolean roundOffActivationValues = false; 	// Whether to round off neuron values
	private int precision = 0; // Degree to which to round off values
	private Network parentNet = null; //Only useed for sub-nets of complex networks which have parents
	
	public Network() {
	}
	
	public abstract void update();
	
	
	/**
	 * Initialize the network.
	 *
	 */
	public void init() {
		initInputsOutputs();;
		initWeights();
	}
	
	/**
	 * Update input and output lists to reflect any newly added input or output nodes
	 *
	 */
	public void initInputsOutputs() {
		inputList.clear();
		outputList.clear();
		//initialize input and output lists
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = getNeuron(i);
			n.init();
			n.setNeuronParent(this);
			if (n.isInput()) {
				inputList.add(n);
			}
			if (n.isOutput()) {
				outputList.add(n);
			}
		}
	}
	
	/**
	 * Updates weights with fan-in.  Used when weights have been added.
	 *
	 */
	public void initWeights() {		
		//initialize fan-in and fan-out on each neuron
		for (int i = 0; i < weightList.size(); i++) {
			Synapse w = getWeight(i);
			w.init();
		}
		
	}
	
	
	/**
	 * @return the name of the class of this network
	 */
	public String getType() {
		return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
	}
	
	/** 
	 * @return how many subnetworks down this is
	 */
	public int getDepth() {
		Network net = this;
		int n = 0;
		while(net != null) {
			net = net.getNetworkParent();
			n++;
		}	
		return n;
	}
	
	/**
	 * @return a string of tabs for use in indenting debug info accroding to the depth of a subnet
	 */
	public String getIndents() {
		String ret = new String("");
		for(int i = 0; i < this.getDepth()-1; i++ ) {
			ret = ret.concat("\t");
		}
		return ret;
	}
	
	public Collection getNeuronList() {
		return this.neuronList;
	}

	public Collection getWeightList() {
		return this.weightList;
	}

	public int getNeuronCount() {
		return neuronList.size();
	}

	public Neuron getNeuron(int index) {
		return (Neuron) neuronList.get(index);
	}

	public void addNeuron(Neuron neuron) {
		neuron.setNeuronParent(this);
		neuronList.add(neuron);		
	}

	public int getWeightCount() {
		return weightList.size();
	}

	public Synapse getWeight(int index) {
		return (Synapse) weightList.get(index);
	}
	
	public int getTime() {
		return time;
	}

	public void setTime(int i) {
		time = i;
	}


	/**
	 * Adds a weight to the neuron network, where that weight already
	 * has designated source and target neurons
	 *
	 * @param weight the weight object to add
	 */
	public void addWeight(Synapse weight) {
		Neuron source = (Neuron) weight.getSource();
		source.addTarget(weight);
		Neuron target = (Neuron) weight.getTarget();
		target.addSource(weight);
		weightList.add(weight);
	}

	/**
	 * Calls {@link Neuron#update} for each neuron
	 */
	public void updateAllNeurons() {
		// First update the activation buffers
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.update();		// update neuron buffers
		}
		// Then update the activations themselves
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.setActivation(n.getBuffer());
		}

	}
	
	/**
	 * Calls {@link Weight#update} for each weight
	 */
	public void updateAllWeights() {
		// No Buffering necessary because the values of weights don't depend on one another
		for (int i = 0; i < weightList.size(); i++) {
			Synapse w = (Synapse) weightList.get(i);
			w.update();
		}

	}
	/**
	 * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the neuron
	 * has not exceeded its upper bound or gone below its lower bound. 
	 * 
	 * TODO: Add or replace with normalization within bounds?
	 */
	public void checkAllBounds() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron n = (Neuron) neuronList.get(i);
			n.checkBounds();
		}
		for (int i = 0; i < weightList.size(); i++) {
			Synapse w = (Synapse) weightList.get(i);
			w.checkBounds();
		}		

	}

	/**
	 * Decay each neuron using its decay method
	 * 
	 * @see Neuron#decay()
	 */
	public void decayAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.decay();
		}
	}

	/**
	 * Bias each neuron using its decay method
	 * 
	 * @see Neuron#bias()
	 */
	public void biasAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.bias();
		}
	}	

	//Round activatons off to integers; for testing	
	public void roundAll() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.round(precision);
		}
	}	
	
	
	/**
	 * Deletes a neuron from the network
	 * 
	 * @param toDelete neuron to delete
	 */
	public void deleteNeuron(Neuron toDelete) {
		if (neuronList.contains(toDelete)) {
			if(inputList.contains(toDelete)) {
				removeInputNeuron(toDelete);
			}
			if(outputList.contains(toDelete)) {
				removeOutputNeuron(toDelete);
			}
			for(int i = 0; i < toDelete.getFanOut().size(); i++) {
				Synapse w = (Synapse) toDelete.getFanOut().get(i);
				deleteWeight(w);
			}
			toDelete.getFanOut().clear();
			for(int i = 0; i < toDelete.getFanIn().size(); i++) {
				Synapse w = (Synapse) toDelete.getFanIn().get(i);
				deleteWeight(w);
			}			
			toDelete.getFanIn().clear();
			neuronList.remove(toDelete);
		}
		
		

	}
	
	/**
	 * Wipe out the whole network
	 */
	public void deleteAllNeurons() {

		weightList.clear();
		neuronList.clear();
		inputList.clear();
		outputList.clear();

	}

	/**
	 * Delete a specified weight 
	 * 
	 * @param toDelete the  Weight to delete
	 */
	public void deleteWeight(Synapse toDelete) {
		toDelete.getSource().getFanOut().remove(toDelete);
		toDelete.getTarget().getFanIn().remove(toDelete);
		weightList.remove(toDelete);
		if(this.getNetworkParent() != null) {
			getNetworkParent().deleteWeight(toDelete);
		}


	}

	/**
	 * Set the activation level of all neurons to zero
	 */
	public void setNeuronsToZero() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.setActivation(0);
		}
	}

	/**
	 * Returns the "state" of the network--the activation level of its neurons.  Used by the
	 * gauge component
	 *
	 * @return an array representing the activation levels of all the neurons in this network
	 */
	public double[] getState() {
		double ret[] = new double[this.getNeuronCount()];

		for (int i = 0; i < this.getNeuronCount(); i++) {
			Neuron n = getNeuron(i);
			ret[i] = (int) n.getActivation();
		}

		return ret;
	}

	/**
	 * Adds the specified neuron to the list of input neurons
	 * 
	 * @param n input neuron to add
	 */
	public void addInputNeuron(Neuron n) {
		inputList.add(n);
	}

	/**
	 * Adds the specified neuron to the list of input neurons
	 * 
	 * @param n input neuron to add
	 */
	public void addOutputNeuron(Neuron n) {
		outputList.add(n);
	}
		
	/**
	 * Removes the specified neuron from the list of input neurons
	 * 
	 * @param n input neuron to remove
	 */
	public void removeInputNeuron(Neuron n) {
		inputList.remove(n);	
	}

	/**
	 * Removes the specified neuron from the list of input neurons
	 * 
	 * @param n input neuron to remove
	 */
	public void removeOutputNeuron(Neuron n) {
		outputList.remove(n);		
	}	
	
	// update input and output lists based on input and output indices of neurons
	// used when reading in savednetworks
	public void updateInOut() {
		inputList.clear();
		outputList.clear();
		
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron)neuronList.get(i);
			if (temp.isInput()) {
				addInputNeuron(temp);
			}
			if (temp.isOutput()) {
				addOutputNeuron(temp);
			}
		}
	}

	/**
	 * @return list of output neurons
	 */
	public ArrayList getOutputs() {
		return outputList;
	}

	/**
	 * Same as getOutputs but returns an array of doubles
	 * 
	 * @return array of output neurons
	 */
	public double[] getOutputArray() {
		double[] ret = new double[outputList.size() + 1];
		Neuron temp = null;
		for (int i = 0; i < outputList.size(); i++) {
			temp = (Neuron) outputList.get(i);
			ret[i] = temp.getActivation();
		}

		return ret;
	}

	/**
	 * return a list of input neurons
	 */
	public ArrayList getInputs() {
		return inputList;
	}

	/**
	 * Same as getInputs but returnss an array of doubles
	 * 
	 * @return array of input neurons
	 */
	public double[] getInputsD() {
		double[] ret = new double[inputList.size()];
		Neuron temp = null;
		for (int i = 0; i < inputList.size(); i++) {
			temp = (Neuron) inputList.get(i);
			ret[i] = temp.getActivation();
		}

		return ret;
	}

	/**
	 * Returns the highest input index.  Used when adding new input nodes, so that new indices are created just above the old ones
	 * @return the highest index among the set of input nodes.
	 */
	public int getLargestInputIndex() {
		return inputList.size() + 1;
		
		//TODO: Fix this!
	}		
		

	
	/**
	 * @param inputList The inputList to set.
	 */

	/**
	 * Sets the input layer of neurons to values specified by some external source,
	 * currently simulated smell stimuli from the World Component
	 *
	 * @param sensorium external vector of values for the network's "sensorium"
	 * @see org.simbrain.sim.world.World
	 */
	public void setInputs(double[] sensorium) {
		Neuron temp = null;
		double val = 0;

		int num =
			(sensorium.length < inputList.size())
				? sensorium.length
				: inputList.size();

		for (int i = 0; i < num; i++) {
			temp = (Neuron) inputList.get(i);
			val = sensorium[i];
			temp.setActivation(val);
		}
	}

	/**
	 * Sets all weight values to zero, effectively eliminating them
	 */
	public void setWeightsToZero() {
		for (int i = 0; i < weightList.size(); i++) {
			Synapse temp = (Synapse) weightList.get(i);
			temp.setStrength(0);
		}
	}

	/**
	 * Randomizes all neurons.
	 */
	public void randomizeNeurons() {
		for (int i = 0; i < neuronList.size(); i++) {
			Neuron temp = (Neuron) neuronList.get(i);
			temp.randomize();
		}
	}

	/**
	 * Randomizes all weights.
	 */
	public void randomizeWeights() {
		for (int i = 0; i < weightList.size(); i++) {
			Synapse temp = (Synapse) weightList.get(i);
			temp.randomize();
		}
		//Must make this symmetrical
	}


	/**
	 * Round a value off to indicated number of decimal places
	 * 
	 * @param value value to round off
	 * @param decimalPlace degree of precision
	 * @return rounded number
	 */
	 public static double round(double value, int decimalPlace) {
		double power_of_ten = 1;
		while (decimalPlace-- > 0)
		   power_of_ten *= 10.0;
		return Math.round(value * power_of_ten) / power_of_ten;
	} 

	/**
	 * Sends relevant information about the network to standard output.
	 */
	public void debug() {

		if (neuronList.size() > 0 ) {
			for (int i = 0; i < neuronList.size(); i++) {
				Neuron tempRef = (Neuron) neuronList.get(i);
				System.out.println(getIndents() + "Neuron " + tempRef.getId() + ": " +  tempRef.getActivation());
			}			
		}
		
		if (weightList.size() > 0 ) {
			for (int i = 0; i < weightList.size(); i++) {
				Synapse tempRef = (Synapse) weightList.get(i);
				System.out.print(getIndents() + "Weight [" + i + "]: " + tempRef.getStrength() + ".");
				System.out.println(
					"  Connects neuron "
						+ tempRef.getSource().getId()
						+ " to neuron "
						+ tempRef.getTarget().getId());
			}			
		}
		
		if(inputList.size() > 0) {
			for (int i = 0; i < inputList.size(); i++) {
				Neuron tempRef = (Neuron) inputList.get(i);
				System.out.println(getIndents() + "Input [" + i + "] (Neuron " + tempRef.getId() + "): " + tempRef.getActivation());
			}			
		}
		
		if(outputList.size() > 0 ) {
			for (int i = 0; i < outputList.size(); i++) {
				Neuron tempRef = (Neuron) outputList.get(i);
				System.out.println(getIndents() + "Output [" + i + "]:" + tempRef.getActivation());
			}			
		}

	}


	public int getPrecision() {
		return precision;
	}

	public boolean isRoundingOff() {
		return roundOffActivationValues;
	}

	public void setPrecision(int i) {
		precision = i;
	}

	public void setRoundingOff(boolean b) {
		roundOffActivationValues = b;
	}
	

	/**
	 * @return Returns the roundOffActivationValues.
	 */
	public boolean isRoundOffActivationValues() {
		return roundOffActivationValues;
	}
	/**
	 * @param roundOffActivationValues The roundOffActivationValues to set.
	 */
	public void setRoundOffActivationValues(boolean roundOffActivationValues) {
		this.roundOffActivationValues = roundOffActivationValues;
	}
	/**
	 * @param inputs The inputs to set.
	 */
	public void setInputs(ArrayList inputs) {
		this.inputList = inputs;
	}
	/**
	 * @param neuronList The neuronList to set.
	 */
	public void setNeuronList(ArrayList neuronList) {
		System.out.println("-->" + neuronList.size());
		this.neuronList = neuronList;
	}

	/**
	 * @param outputs The outputs to set.
	 */
	public void setOutputs(ArrayList outputs) {
		this.outputList = outputs;
	}
	/**
	 * @param weightList The weightList to set.
	 */
	public void setWeightList(ArrayList weightList) {
		this.weightList = weightList;
	}
	
	/**
	 * Set bias values for all neurons in this network
	 * 
	 * @param biases array of new bias values
	 */
	public void setBiases(double[] biases) {
		if (biases.length != getNeuronCount()) {
			System.out.println("Invalid argument to setBiases");
			return;
		}
		
		for (int i = 0; i < getNeuronCount(); i++) {
			getNeuron(i).setBias(biases[i]);
		}
	}
	
	
	public double[] getBiases() {
		double[] ret = new double[getNeuronCount()];
		for (int i = 0; i < getNeuronCount(); i++) {
			ret[i] = getNeuron(i).getBias();
		}
		return ret;
		
	}
	
	/**
	 * Add an array of neurons and set their parents to this
	 * 
	 * @param neurons list of neurons to add
	 */
	public void addNeuronList(ArrayList neurons) {
		for(int i = 0; i < neurons.size(); i++) {
			Neuron n = (Neuron)neurons.get(i);
			n.setNeuronParent(this);
			neuronList.add(n);
		}
	}
	
	/**
	 * Set activation rule for every neuron in the network
	 * 
	 * @param rule the name of the rule to set the neurons to
	 */
	public void setRules(String rule) {
		for (int i = 0; i < getNeuronCount(); i++) {
			getNeuron(i).setActivationFunction(ActivationRule.getActivationFunction(rule));
		}		
	}
	
	public void setUpperBounds(double u) {
		for (int i = 0; i < getNeuronCount(); i++) {
			getNeuron(i).setUpperBound(u);
		}

	}

	public void setLowerBounds(double l) {
		for (int i = 0; i < getNeuronCount(); i++) {
			getNeuron(i).setUpperBound(l);
		}

	}
	
	/**
	 * Returns a reference to the synapse connecting two neurons, or null if there is none
	 * @param src source neuron
	 * @param tar target neuron
	 * @return synapse from source to target
	 */
	public static Synapse getWeight(Neuron src, Neuron tar) {
		for (int i = 0; i < src.fanOut.size(); i++) {
			Synapse s = (Synapse)src.fanOut.get(i);
			if (s.getTarget() == tar) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Replace one neuron with another
	 * 
	 * @param old_neuron out with the old
	 * @param new_neuron in with the new...
	 */
	public static void changeNeuron(Neuron old_neuron, Neuron new_neuron) {
		new_neuron.setFanIn(old_neuron.getFanIn());
		new_neuron.setFanOut(old_neuron.getFanOut());
		new_neuron.setNeuronParent(old_neuron.getNeuronParent());
		for(int i = 0; i < old_neuron.getFanIn().size(); i++) {
			((Synapse)old_neuron.getFanIn().get(i)).setTarget(new_neuron);
		}
		for(int i = 0; i < old_neuron.getFanOut().size(); i++) {
			((Synapse)old_neuron.getFanOut().get(i)).setSource(new_neuron);
		}		
		old_neuron.getNeuronParent().getNeuronList().remove(old_neuron);
		old_neuron.getNeuronParent().getNeuronList().add(new_neuron);
		new_neuron.getNeuronParent().initInputsOutputs();

	}
	
	
	//TODO: Either fix this or make its assumptions explicit
	public Synapse getWeight(int i, int j) {
		return (Synapse)getNeuron(i).getFanOut().get(j);
	}
	/**
	 * @return Returns the parentNet.
	 */
	public Network getNetworkParent() {
		return parentNet;
	}
	/**
	 * @param parentNet The parentNet to set.
	 */
	public void setNetworkParent(Network parentNet) {
		this.parentNet = parentNet;
	}
}
